package com.example.samuraitravel.controller;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.Loves;
import com.example.samuraitravel.entity.Review;
import com.example.samuraitravel.entity.User;
import com.example.samuraitravel.form.ReservationInputForm;
import com.example.samuraitravel.form.ReviewRegisterForm;
import com.example.samuraitravel.repository.HouseRepository;
import com.example.samuraitravel.repository.LovesRepository;
import com.example.samuraitravel.repository.ReviewsRepository;
import com.example.samuraitravel.repository.RoleRepository;
import com.example.samuraitravel.repository.UserRepository;
import com.example.samuraitravel.security.UserDetailsImpl;
import com.example.samuraitravel.service.ReviewsService;
import com.example.samuraitravel.service.UserService;
//import com.stripe.service.ReviewService;


@Controller
@RequestMapping("/houses")
public class HouseController {
    private static final RoleRepository RoleRepository = null;
	private final HouseRepository houseRepository;
    private final UserRepository userRepository; // UserRepositoryの追加
    private final ReviewsRepository reviewsRepository; // ReviewsRepositoryの追加
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final LovesRepository lovesRepository;
    
    @Autowired
    private ReviewsService reviewService;

    @Autowired
    private UserService userService; // UserService を追加
    
    public HouseController(HouseRepository houseRepository0, UserRepository userRepository0, ReviewsRepository reviewsRepository0, LovesRepository lovesRepository0) {
        this.houseRepository = houseRepository0;
        this.userRepository = userRepository0; // UserRepositoryの初期化
        this.reviewsRepository = reviewsRepository0; // ReviewsRepositoryの初期化
		this.roleRepository = null;
		this.passwordEncoder = null;
		this.lovesRepository = lovesRepository0;		
    }     
  
    @GetMapping
    public String index(@RequestParam(name = "keyword", required = false) String keyword,
                        @RequestParam(name = "area", required = false) String area,
                        @RequestParam(name = "price", required = false) Integer price,  
                        @RequestParam(name = "order", required = false) String order,
                        @PageableDefault(page = 0, size = 5, sort = "id", direction = Direction.ASC) Pageable pageable,
                        Model model){

        Page<House> housePage;
                
        if (keyword != null && !keyword.isEmpty()) {            
            if (order != null && order.equals("priceAsc")) {
                housePage = houseRepository.findByNameLikeOrAddressLikeOrderByPriceAsc("%" + keyword + "%", "%" + keyword + "%", pageable);
            } else {
                housePage = houseRepository.findByNameLikeOrAddressLikeOrderByCreatedAtDesc("%" + keyword + "%", "%" + keyword + "%", pageable);
            }            
        } else if (area != null && !area.isEmpty()) {            
            if (order != null && order.equals("priceAsc")) {
                housePage = houseRepository.findByAddressLikeOrderByPriceAsc("%" + area + "%", pageable);
            } else {
                housePage = houseRepository.findByAddressLikeOrderByCreatedAtDesc("%" + area + "%", pageable);
            }            
        } else if (price != null) {            
            if (order != null && order.equals("priceAsc")) {
                housePage = houseRepository.findByPriceLessThanEqualOrderByPriceAsc(price, pageable);
            } else {
                housePage = houseRepository.findByPriceLessThanEqualOrderByCreatedAtDesc(price, pageable);
            }            
        } else {            
            if (order != null && order.equals("priceAsc")) {
                housePage = houseRepository.findAllByOrderByPriceAsc(pageable);
            } else {
                housePage = houseRepository.findAllByOrderByCreatedAtDesc(pageable);   
            }            
        }                
        
        model.addAttribute("housePage", housePage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("area", area);
        model.addAttribute("price", price); 
        model.addAttribute("order", order);
        
        return "houses/index";
    }
    
    @GetMapping("/{id}")
    public String show(
            @PathVariable(name = "id") Integer id,
            @PageableDefault(page = 0, size = 5, sort = "updatedAt", direction = Direction.ASC) Pageable pageable,
            Model model
            /* @AuthenticationPrincipal UserDetailsImpl userDetails */
    ) {

        //UserDetailsImpl userDetails = null;
    	UserDetailsImpl userDetails=null;
    	Boolean loginwas = false;
    	Boolean likeThis = false;
    	
    	try {
    		userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                userDetails = (UserDetailsImpl) authentication.getPrincipal();
                loginwas = true;
            } else {
                // 未認証の場合の処理
            }
        } catch (ClassCastException e) {
            // UserDetailsImplへのキャストに失敗した場合の処理
        }
    	
        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetailsImpl) {
            userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }

        boolean hasWrittenReview = false; // スコープの外で宣言
        
        if (userDetails != null) {
        	UserDetailsImpl userDetailsCopy = userDetails;
            System.out.println(userDetails.getUserId()); // 現在ログインしているユーザーのuserId
            System.out.println(userDetails.getPassword()); // 現在ログインしているユーザーのハッシュ化されたpassword
            System.out.println(userDetails.getUsername()); // 現在ログインしているユーザーのmailアドレス

            // 現在のユーザーのレビューを取得
            hasWrittenReview = reviewsRepository.findByHouseIdOrderByCreatedAtDesc(id)
                    .stream()
                    .anyMatch(r -> r.getUserid().equals(userDetailsCopy.getUserId())); // ユーザーのレビューがあるか確認
            model.addAttribute("currentUserId", userDetails.getUserId());
            
            //お気に入り情報
            Pageable pageableLove = null;
            System.out.println("  Loves Size = " + lovesRepository.findByUseridAndHouseidOrderByCreatedAtDesc(userDetails.getUserId(), id, pageableLove).getContent().size());
            System.out.println("  Loves Tostring = " + lovesRepository.findByUseridAndHouseidOrderByCreatedAtDesc(userDetails.getUserId(), id, pageableLove).getContent().toString());
            if (lovesRepository.findByUseridAndHouseidOrderByCreatedAtDesc(userDetails.getUserId(), id, pageableLove).getContent().size()>0 ) {
            	likeThis = true;
            }else {
            	likeThis = false;
            }
            
        } else {
            System.out.println(" ##### NOT LOGIN #####");
        }
        
        System.out.println("  Loves likeThis = " + likeThis);
        model.addAttribute("likeThis", likeThis);

        ReviewsService reviewsService = new ReviewsService(reviewsRepository);
        reviewsService.refreshReviews();

        model.addAttribute("doesReviewExist", userRepository.existsById(id));

        // 表示中houseの物件詳細データ
        House house = houseRepository.getReferenceById(id);

        List<User> userAll = userRepository.findAll(); // 有効なユーザーのリストを取得
        model.addAttribute("loginwas", loginwas);
        System.out.println("loginwas= " + loginwas);
        System.out.println("hasWrittenReview= " + hasWrittenReview);
        System.out.println("Userall= " + userAll);
        if (!userAll.isEmpty()) {
            System.out.println("Userall.get(0)= " + userAll.get(2).getName());
        }

        // Houseに関連するレビューを取得
        //List<Review> reviews = reviewsRepository.findByHouseIdOrderByCreatedAtDesc(id);
        //Page<Review> reviewsPage = reviewsRepository.findByHouseIdOrderByCreatedAtDesc(id,pageable);
        List<Review> reviews = reviewsRepository.findByHouseIdOrderByCreatedAtDesc(id);
        //model.addAttribute("reviewsPage", reviewsPage);
        System.out.println("   reviews= " + reviews);
        Map<Integer, Map<String, String>> reviewsWithUserName = new HashMap<>();
        
        if(reviews.size()>0) {
        	model.addAttribute("haveReviews", reviews.size());
	        for (int i = 0; i < reviews.size(); i++) {
	            Review review = reviews.get(i);
	            Map<String, String> reviewDetails = new HashMap<>();
	            reviewDetails.put("content", String.valueOf(review.getReviewText()));
	            reviewDetails.put("rating", String.valueOf(review.getRating()));
	            reviewDetails.put("houseid", String.valueOf(review.getHouseid()));
	            reviewDetails.put("userid", String.valueOf(review.getUserid()));
	            reviewDetails.put("reviewid", String.valueOf(review.getId()));
	
	            String user = userService.getUserNameById(review.getUserid());
	            String isUserExistencee = (user != null) ? userService.getUserNameById2(review.getUserid()) : "退会ユーザー";
	
	            System.out.println("review[isUserExistencee]= " + isUserExistencee);
	            reviewDetails.put("isUserExistencee", isUserExistencee);
	            reviewsWithUserName.put(i, reviewDetails);
	            System.out.println("   i= " + i);
	        }
        }else {
        	model.addAttribute("haveReviews", 0);
        }

        model.addAttribute("reviewsWithUserName", reviewsWithUserName);
        System.out.println("reviewsWithUserName= " + reviewsWithUserName.toString());
        

        User userLogined = null;
        if (userDetails != null) {
            userLogined = userRepository.getReferenceById(userDetails.getUserId());
        }
        User user = userRepository.getReferenceById(id);

        boolean isAuthenticatedData = false;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
        	isAuthenticatedData = true;
        }
        System.out.println("isAuthenticatedData= " + isAuthenticatedData);
        model.addAttribute("isAuthenticatedData", isAuthenticatedData);
        
        model.addAttribute("house", house);
        model.addAttribute("reservationInputForm", new ReservationInputForm());
        model.addAttribute("reviewRegisterForm", new ReviewRegisterForm());
        model.addAttribute("reviews", reviews);
        model.addAttribute("user", user);
        model.addAttribute("userAll", userAll);
        model.addAttribute("houseName", house.getName());
        model.addAttribute("houseID", house.getId());

        if (userDetails != null) {
            model.addAttribute("hasWrittenReview", hasWrittenReview);
            if (userLogined != null) {
                model.addAttribute("userLogined", userLogined);
            };
        };
	    		 
        
        return "houses/show";
    }
    
     
     //@GetMapping("/houses/{houseId}/review/{reviewId}/delete")
     //@GetMapping("/{houseId}/review/{reviewId}/delete")
     @PostMapping("/{houseId}/review/delete")
     public String delete(
    		 @PathVariable(name = "houseId") Integer houseId,
    		 //@PathVariable(name = "reviewId") Integer reviewId,
    		 @RequestParam("reviewIdData") Integer reviewId,
    		 @RequestParam("valueData") String valueDelete,
    		 Model model,
    		 /* @AuthenticationPrincipal UserDetailsImpl userDetails */
    		 RedirectAttributes redirectAttributes){
    	 
    	 UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    	 
    	 System.out.println("   ***** review delete *****");
    	 System.out.println("houseId= " + houseId);
    	 System.out.println("reviewId= " + reviewId);  
    	 System.out.println("valueDelete= " + valueDelete);
    	 System.out.println(houseId+"/review/"+reviewId+"/delete");
    	 System.out.println("   *************************");
    	 
    	 List <Review> reviews = reviewsRepository.findByHouseIdOrderByCreatedAtDesc(houseId);
    	 
    	 // レビューをデータベースから取得
    	 Review existingReview = reviewsRepository.findById(reviewId).orElseThrow(() -> new NoSuchElementException("Review not found with id " + reviewId));
    	 // ユーザーがレビューの所有者であることを確認
 	     if (!existingReview.getUserid().equals(userDetails.getUserId())) {
 	    	System.out.println("ユーザーがレビューの所有者であることを確認= NG");
 	         throw new AccessDeniedException("You do not have permission to edit this review.");
 	     }
 	     System.out.println("ユーザーがレビューの所有者であることを確認= OK");
 	     
 	     if(valueDelete.contains("delete") && valueDelete.length()==6) {
	 	     //レビュー削除実行
	    	 reviewsRepository.deleteById(reviewId);	    	 
	    	 redirectAttributes.addFlashAttribute("successMessage", "レビューを削除しました。");
 	     }else {
 	    	redirectAttributes.addFlashAttribute("successMessage", "レビュー削除エラー");
 	     } 	    
    	 
    	 //return "houses/show";
    	 //return "redirect:/houses/{houseId}";
 	     //return "houses/{houseId}";
 	     String retStr = "redirect:/houses/" + houseId;
	     return retStr;
     }
         
     //@RequestMapping("/houses/{houseId}/review/{reviewId}/edit")
     //@PostMapping("/houses/{houseId}/review/{reviewId}/edit")
     //@PostMapping("/{houseId}/review/{reviewId}/edit")
     @PostMapping("/{houseId}/review/edit")
     public String edit (
    		 @PathVariable(name = "houseId") Integer houseId,
    		 //@PathVariable(name = "reviewId") Integer reviewId,
    		 @RequestParam("reviewIdData") Integer reviewId,
    		 @RequestParam("contentChange") String fixContent,
    		 @RequestParam("ratingChange") Integer fixRating,
    		 /* @AuthenticationPrincipal UserDetailsImpl userDetails, */
    		 Model model,
    		 RedirectAttributes redirectAttributes) {
    	 
    	 UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    	 
    	 System.out.println("houseId= " + houseId);
    	 System.out.println("reviewId= " + reviewId);
    	 System.out.println("fixContent= " + fixContent);
    	 System.out.println("fixRating= " + fixRating);
    	 System.out.println(houseId+"/review/"+reviewId+"/edit");
    	 
    	 List <Review> reviews = reviewsRepository.findByHouseIdOrderByCreatedAtDesc(houseId);
    	 
    	 // レビューをデータベースから取得
    	 Review existingReview = reviewsRepository.findById(reviewId).orElseThrow(() -> new NoSuchElementException("Review not found with id " + reviewId));
    	 // ユーザーがレビューの所有者であることを確認
 	     if (!existingReview.getUserid().equals(userDetails.getUserId())) {
 	    	System.out.println("ユーザーがレビューの所有者であることを確認= NG");
 	         throw new AccessDeniedException("You do not have permission to edit this review.");
 	     }
 	     System.out.println("ユーザーがレビューの所有者であることを確認= OK");
 	     
 	     // レビューの内容と評価を更新
		 existingReview.setReviewText(fixContent);
		 existingReview.setRating(fixRating);
		 existingReview.setUpdatedAt(new Timestamp(System.currentTimeMillis())); // 更新日時を現在の日時に設定

 	     // 更新を保存
 	     reviewsRepository.save(existingReview);

 	     // モデルに必要なデータを追加（例: 更新後のレビューを表示するため）
 	     //model.addAttribute("review", fixContent);
 	     //model.addAttribute("houseId", houseId);
 	     redirectAttributes.addFlashAttribute("successMessage", "レビューを修正しました。");
    	 
 	     //return "houses/show";
 	     //return "redirect:/houses/{houseId}"; 	     
 	     //return "houses/{houseId}";
 	     String retStr = "redirect:/houses/" + houseId;
 	     return retStr;
     }
     
     @PostMapping("/{houseId}/review/{userId}/write")
     public String write(
    		 @PathVariable(name = "houseId") Integer houseId,
    		 @PathVariable(name = "userId") Integer userId,
    		 @ModelAttribute @Validated ReviewRegisterForm reviewRegisterForm, 
    		 BindingResult bindingResult, 
    		 RedirectAttributes redirectAttributes
	 ) {
    	// フォームデータをエンティティに変換
         Review review = new Review();
         review.setRating(reviewRegisterForm.getRating());
         review.setReviewText(reviewRegisterForm.getReviewText());
         review.setUserId(reviewRegisterForm.getReviewUserId());
         review.setHouseId(reviewRegisterForm.getReviewhouseId());
         
         /*
         if (bindingResult.hasErrors()) {
        	 redirectAttributes.addFlashAttribute("errorMessage", "レビュー登録に失敗しました。");    
        	 List<ObjectError> errors = bindingResult.getAllErrors();
        	 List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        	 for (ObjectError error : errors) {
        		    String errorMessage = error.getDefaultMessage(); // エラーメッセージを取得
        		    System.out.println("　　$$$$$$ Error: " + errorMessage);
    		}
        	 for (FieldError fieldError : fieldErrors) {
        		    String fieldName = fieldError.getField(); // フィールド名を取得
        		    String errorMessage = fieldError.getDefaultMessage(); // エラーメッセージを取得
        		    System.out.println("　　$$$$$$ Field: " + fieldName + ", Error: " + errorMessage);
    		}
             String retStr = "redirect:/houses/" + houseId;
     	     return retStr;
         }
         */
         
         reviewService.write(reviewRegisterForm);
         redirectAttributes.addFlashAttribute("successMessage", "レビューを登録しました。");    
         
         String retStr = "redirect:/houses/" + houseId;
 	     return retStr;
     }
     
     
     @GetMapping("/{houseId}/favorite")
     public String favorite(@PathVariable(name = "houseId") Integer houseId) {
    	 Pageable pageable = null;
    	 String urlStr = "/{houseId}/#";
    	 UserDetailsImpl userDetails=null;
      	 try {
      		userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
              if (authentication != null && authentication.isAuthenticated()) {
                  userDetails = (UserDetailsImpl) authentication.getPrincipal();
              } else {
                  // 未認証の場合の処理
            	  return "/{houseId}/#";
		      }
		 } catch (ClassCastException e) {
		      // UserDetailsImplへのキャストに失敗した場合の処理
		 }
		
		 if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetailsImpl) {
		      userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		 }
		  
		 if (userDetails != null) {
		  	UserDetailsImpl userDetailsCopy = userDetails;
	  		if (lovesRepository.findByUseridAndHouseidOrderByCreatedAtDesc(userDetails.getUserId(), houseId, pageable).getContent().size()>0 ) {
	  			urlStr = "/" + houseId + "/favorite/" + userDetails.getUserId() + "/delete";
            }else {
            	urlStr = "/" + houseId + "/favorite/" + userDetails.getUserId() + "/add";
            }

		 }
		 return urlStr;
     }
     
     //@PostMapping("/{houseId}/favorite/{userId}/add")
     @GetMapping("/{houseId}/favorite/{userId}/add")
     public String add(
    		 @PathVariable(name = "houseId") Integer houseId,
    		 @PathVariable(name = "userId") Integer userId,
    		 RedirectAttributes redirectAttributes
	 ) {
    	// エンティティに変換
         Loves loves = new Loves();
         loves.setHouseid(houseId);
         loves.setUserid(userId);
         
         lovesRepository.save(loves);
         redirectAttributes.addFlashAttribute("successMessage", "	お気に入り登録しました。");    
         
         String retStr = "redirect:/houses/" + houseId;
 	     return retStr;
     }
     
     //@PostMapping("/{houseId}/favorite/{userId}/remove")
     @GetMapping("/{houseId}/favorite/{userId}/remove")
     public String remove(
    		 @PathVariable(name = "houseId") Integer houseId,
    		 @PathVariable(name = "userId") Integer userId,
    		 RedirectAttributes redirectAttributes
	 ) {
    	 System.out.println(" ##### favorite #####  DELETE");
    	Pageable pageable = null;
    	UserDetailsImpl userDetails=null;     	
     	try {
     		userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
     		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
     		 System.out.println(" ##### favorite #####  DELETE 1");
             if (authentication != null && authentication.isAuthenticated()) {
                 userDetails = (UserDetailsImpl) authentication.getPrincipal();
                 System.out.println(" ##### favorite #####  DELETE 2");
             } else {
                 // 未認証の場合の処理
            	 System.out.println(" ##### favorite #####  DELETE 3");
             }
         } catch (ClassCastException e) {
             // UserDetailsImplへのキャストに失敗した場合の処理
        	 System.out.println(" ##### favorite #####  DELETE 4");
         }
     	
         if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetailsImpl) {
        	 System.out.println(" ##### favorite #####  DELETE 5");
             userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
         }
         
         if (userDetails != null) {
        	 System.out.println(" ##### favorite #####  DELETE 6");
         	UserDetailsImpl userDetailsCopy = userDetails;
         	if(userId!=userDetails.getUserId()) {
         		 System.out.println(" ##### favorite #####  DELETE 7");
         		throw new AccessDeniedException("You do not have permission to remove this favorite.");
	         }else{
	        	 System.out.println(" ##### favorite #####  DELETE 8");
	         	// エンティティに変換
	        	 Loves loves = new Loves();
	        	 if(lovesRepository.findByUseridAndHouseidOrderByCreatedAtDesc(userDetails.getUserId() ,houseId ,pageable).getContent().size()==1) {
	        		 System.out.println(" ##### favorite #####  DELETE 9");
	        		 loves = lovesRepository.findByUseridAndHouseidOrderByCreatedAtDesc(userDetails.getUserId() ,houseId ,pageable).getContent().get(0);
	        	 }else {
	        		 System.out.println(" ##### favorite #####  DELETE 10");
	        		 throw new AccessDeniedException("Many favorite.");
	        	 }
	        	 
	        	 System.out.println(" ##### favorite #####  DELETE 11");
	        	 System.out.println("   loves.getId()= " + loves.getId());
	        	 
	             lovesRepository.deleteById(loves.getId());
	             redirectAttributes.addFlashAttribute("successMessage", "	お気に入り解除しました。");
	         }
         }
         String retStr = "redirect:/houses/" + houseId;
 	     return retStr;
     }
     
     /*
     @PostMapping("/{houseId}/review/{userId}/write")
     public String write (
    		 @PathVariable(name = "houseId") Integer houseId,
    		 @PathVariable(name = "userId") Integer userId,
    		 @RequestParam("contentChange") String fixContent,
    		 @RequestParam("ratingChange") Integer fixRating,
    		 //@AuthenticationPrincipal UserDetailsImpl userDetails,
    		 Model model,
    		 RedirectAttributes redirectAttributes) {
    	 
    	 UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    	 
    	 System.out.println("houseId= " + houseId);
    	 System.out.println("userId= " + userId);
    	 System.out.println("fixContent= " + fixContent);
    	 System.out.println("fixRating= " + fixRating);
    	 System.out.println(houseId+"/review/"+userId+"/write");
    	 
    	 List <Review> reviews = reviewsRepository.findByHouseIdOrderByCreatedAtDesc(houseId);
    	 
    	 // レビューをデータベースから取得
    	 Review existingReview = reviewsRepository.findById(reviewId).orElseThrow(() -> new NoSuchElementException("Review not found with id " + reviewId));
    	 // ユーザーがレビューの所有者であることを確認
 	     if (!existingReview.getUserid().equals(userDetails.getUserId())) {
 	    	System.out.println("ユーザーがレビューの所有者であることを確認= NG");
 	         throw new AccessDeniedException("You do not have permission to edit this review.");
 	     }
 	     System.out.println("ユーザーがレビューの所有者であることを確認= OK");
 	     
 	     // レビューの内容と評価を更新
		 existingReview.setReviewText(fixContent);
		 existingReview.setRating(fixRating);
		 existingReview.setUpdatedAt(new Timestamp(System.currentTimeMillis())); // 更新日時を現在の日時に設定

 	     // 更新を保存
 	     reviewsRepository.save(existingReview);

 	     // モデルに必要なデータを追加（例: 更新後のレビューを表示するため）
 	     //model.addAttribute("review", fixContent);
 	     //model.addAttribute("houseId", houseId);
 	     redirectAttributes.addFlashAttribute("successMessage", "レビューを修正しました。");
    	 
 	     //return "houses/show";
 	     //return "redirect:/houses/{houseId}"; 	     
 	     //return "houses/{houseId}";
 	     String retStr = "redirect:/houses/" + houseId;
 	     return retStr;
     }
     */
     
     /*
     @PostMapping("/houses/{houseId}/review/{reviewId}/edit")
     public String edit(
    		 @PathVariable(name = "houseId") Integer houseId,
    		 @PathVariable(name = "reviewId") Integer reviewId,
    		 @RequestBody ReviewDto reviewDto, // ReviewDtoは内容と評価を持つクラス
    		 Model model,
    		 @AuthenticationPrincipal UserDetailsImpl userDetails){
    	 
    	// レビューをデータベースから取得
    	    //Review existingReview = reviewsRepository.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review not found with id " + reviewId));
    	 Review existingReview = reviewsRepository.findById(reviewId).orElseThrow(() -> new NoSuchElementException("Review not found with id " + reviewId));

    	    // ユーザーがレビューの所有者であることを確認
    	    if (!existingReview.getUserid().equals(userDetails.getUserId())) {
    	        throw new AccessDeniedException("You do not have permission to edit this review.");
    	    }

    	    // レビューの内容と評価を更新
    	    existingReview.setReviewText(reviewDto.getContent());
    	    existingReview.setRating(reviewDto.getRating());
    	    existingReview.setUpdatedAt(new Timestamp(System.currentTimeMillis())); // 更新日時を現在の日時に設定

    	    // 更新を保存
    	    reviewsRepository.save(existingReview);

    	    // モデルに必要なデータを追加（例: 更新後のレビューを表示するため）
    	    model.addAttribute("review", existingReview);
    	    model.addAttribute("houseId", houseId);

    	 
    	 
    	 return "houses/show";
     }
     */


}

