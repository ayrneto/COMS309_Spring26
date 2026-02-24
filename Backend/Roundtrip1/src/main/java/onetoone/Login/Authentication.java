package onetoone.Login;

import onetoone.Users.User;

public class Authentication {

    private static String username;
    private String password;
    private static User users;

    private static String success = "{\"message\":\"success\"}";
    private static String failure = "{\"message\":\"failure\"}";

    public Authentication(String username, String password) {
        this.username = username;
        this.password = password;
        users.setEmail(username);
        users.setPasswordHash(password);
    }

    public Authentication(User user) {
        this.users = user;
    }

    static String checkUsername(){
        if(users!=null){
            if(users.getEmail().equals(username))
                return success;

            else
                return failure;
        }
        return failure;
    }

    String checkPassword(){
        if(users!=null){
            if(users.getPasswordHash().equals(password))
                return success;
            else
                return failure;
        }
        return failure;
    }

    boolean checkAuth(){
        return checkUsername().equals(success) && checkPassword().equals(success);
    }
}
