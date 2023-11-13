package nextech.shoploc.controllers.auth;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import nextech.shoploc.domains.enums.UserTypes;
import nextech.shoploc.models.client.ClientRequestDTO;
import nextech.shoploc.models.client.ClientResponseDTO;
import nextech.shoploc.models.user.UserResponseDTO;
import nextech.shoploc.services.auth.EmailSenderService;
import nextech.shoploc.services.auth.VerificationCodeService;
import nextech.shoploc.services.client.ClientService;
import nextech.shoploc.services.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/client")
@AllArgsConstructor
@NoArgsConstructor
public class ClientLoginController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private UserService userService;

    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private VerificationCodeService verificationCodeService;
    @Autowired
    private EmailSenderService emailSenderService;
    private static final String LOGIN_ERROR = "Identifiant ou mot de passe incorrect";
    private static final String REGISTER_ERROR = "L'inscription a échoué. Veuillez réessayer.";
    private static final String UNAUTHORIZED_ERROR = "Merci de vous authentifier pour accéder à cette ressource.";
    private static final String VERIFICATION_CODE_ERROR = "Code de vérification incorrect. Veuillez réessayer.";


    @GetMapping("/login")
    public ResponseEntity<Map<String, Object>> login(HttpSession session) {
        if (sessionManager.isUserConnectedAsClient(session)) {
            Map<String, Object> response = new HashMap<>();
            response.put("url", "/client/dashboard");
            return new ResponseEntity<>(response, HttpStatus.FOUND);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("url", "/client/login");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestParam String email,
                                                     @RequestParam String password, HttpSession session) throws MessagingException {
        ClientResponseDTO clientResponseDTO = clientService.getClientByEmail(email);
        if (clientResponseDTO != null && userService.verifyPassword(password, clientResponseDTO.getPassword())) {
            String verificationCode = verificationCodeService.generateVerificationCode();
            emailSenderService.sendHtmlEmail(email, verificationCode);
            sessionManager.setUserToVerify(email, UserTypes.client.toString(), verificationCode, session);
            Map<String, Object> response = new HashMap<>();
            response.put("url", "/client/verify");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("error", LOGIN_ERROR);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/register")
    public ResponseEntity<Map<String, Object>> register() {
        Map<String, Object> response = new HashMap<>();
        response.put("url", "/client/register");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@ModelAttribute("client") ClientRequestDTO client) {
        ClientResponseDTO ard = clientService.createClient(client);
        Map<String, Object> response = new HashMap<>();
        if (ard == null) {
            response.put("error", REGISTER_ERROR);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } else {
            response.put("url", "/client/login");
            return new ResponseEntity<>(response, HttpStatus.FOUND);
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(HttpSession session) {
        if (sessionManager.isUserConnectedAsClient(session)) {
            Map<String, Object> response = new HashMap<>();
            UserResponseDTO client = userService.getUserByEmail(sessionManager.getConnectedUserEmail(session));
            response.put("object", client);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("error", UNAUTHORIZED_ERROR);
            response.put("url", "/client/login");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        System.out.println("Logout client...");
        sessionManager.setUserAsDisconnected(session);
        Map<String, Object> response = new HashMap<>();
        response.put("url", "/client/login");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestParam String code, HttpSession session) {
        String savedCode = sessionManager.getVerificationCode(session);
        System.out.println("savedCode: " + savedCode);
        System.out.println("code : " + code);
        System.out.println("equals : " + code.equals(savedCode));

        if (code.equals(savedCode)) {
            // Code de vérification valide, accorder une session
            sessionManager.setUserAsConnected(sessionManager.getConnectedUserEmail(session), String.valueOf(UserTypes.client), session);
            Map<String, Object> response = new HashMap<>();
            response.put("url", "/client/dashboard");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            // Code de vérification incorrect, gérer l'erreur
            Map<String, Object> response = new HashMap<>();
            response.put("error", VERIFICATION_CODE_ERROR);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }

}
