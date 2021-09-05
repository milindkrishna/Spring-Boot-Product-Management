package net.codejava;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import com.lowagie.text.DocumentException;

@Controller
public class AppController {

	@Autowired
	private ProductService service; 
	
	@Autowired
    private UserRepository userRepo;
     
	//login, Register, Logout control
	
    @GetMapping("/home")
    public String viewHomePage() {
        return "index";
    }
    
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
         
        return "signup_form";
    }
    
    @PostMapping("/process_register")
    public String processRegister(User user) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
         
        userRepo.save(user);
         
        return "register_success";
    }
    
    
    @GetMapping("/users")
	public String listUsers(Model model) {
		List<User> listUsers = userRepo.findAll();
		model.addAttribute("listUsers", listUsers);
		
		return "users";
	}
    
    //product controller mapping and Crud controll
	
	@RequestMapping("/details")
	public String viewHomePage(Model model , @Param("keyword") String keyword) {
		List<Product> listProducts = service.listAll(keyword);
		model.addAttribute("listProducts", listProducts);
		model.addAttribute("keyword", keyword);
		return "index1";
	}
	
	@RequestMapping("/new")
	public String showNewProductPage(Model model) {
		Product product = new Product();
		model.addAttribute("product", product);
		
		return "new_product";
	}
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String saveProduct(@ModelAttribute("product") Product product) {
		service.save(product);
		
		return "redirect:/";
	}
	
	@RequestMapping("/edit/{id}")
	public ModelAndView showEditProductPage(@PathVariable(name = "id") int id) {
		ModelAndView mav = new ModelAndView("edit_product");
		Product product = service.get(id);
		mav.addObject("product", product);
		
		return mav;
	}
	
	@RequestMapping("/delete/{id}")
	public String deleteProduct(@PathVariable(name = "id") int id) {
		service.delete(id);
		return "redirect:/";		
	}
	
	//CSV Export Feature
	
	@GetMapping("/export")
    public void exportToCSV(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateTime = dateFormatter.format(new Date());
         
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=products_" + currentDateTime + ".csv";
        response.setHeader(headerKey, headerValue);
         
        List<Product> listProducts = service.listAll();
 
        ICsvBeanWriter csvWriter = new CsvBeanWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);
        String[] csvHeader = {"User ID", "Name", "Brand", "Manufactered-Country", "Price(in Rs)"};
        String[] nameMapping = {"id", "name", "brand", "madein", "price"};
         
        csvWriter.writeHeader(csvHeader);
         
        for (Product product : listProducts) {
            csvWriter.write(product, nameMapping);
        }
         
        csvWriter.close();
         
    }
	
	//pdf exporter feature
	         
	    @GetMapping("/pdf")
	    public void exportToPDF(HttpServletResponse response) throws DocumentException, IOException {
	        response.setContentType("application/pdf");
	        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
	        String currentDateTime = dateFormatter.format(new Date());
	         
	        String headerKey = "Content-Disposition";
	        String headerValue = "attachment; filename=products_" + currentDateTime + ".pdf";
	        response.setHeader(headerKey, headerValue);
	         
	        List<Product> listProducts = service.listAll();
	         
	        UserPDFExporter exporter = new UserPDFExporter(listProducts);
	        exporter.export(response);
	         
	    }
	    
	    //import csv to database feature
	    
	    @PostMapping("/upload")
	    public ResponseEntity<ResponseMessage> uploadFile(@RequestParam("file") MultipartFile file) {
	      String message = "";

	      if (CSVHelper.hasCSVFormat(file)) {
	        try {
	        	service.save(file);

	          message = "Uploaded the file successfully: " + file.getOriginalFilename();
	          return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
	        } catch (Exception e) {
	          message = "Could not upload the file: " + file.getOriginalFilename() + "!";
	          return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
	        }
	      }

	      message = "Please upload a csv file!";
	      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseMessage(message));
	    }

	    @GetMapping("/productinfo")
	    public ResponseEntity<List<Product>> getAllProducts() {
	      try {
	        List<Product> listProducts = service.getAllProducts();

	        if (listProducts.isEmpty()) {
	          return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	        }

	        return new ResponseEntity<>(listProducts, HttpStatus.OK);
	      } catch (Exception e) {
	        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
	      }
	    }
	    
	    
	    
}
