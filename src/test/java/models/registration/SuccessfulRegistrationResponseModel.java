package models.registration;

public record SuccessfulRegistrationResponseModel(Integer id, String username, String firstname,
                                                  String lastname, String email, String remoteAddr) {
}
