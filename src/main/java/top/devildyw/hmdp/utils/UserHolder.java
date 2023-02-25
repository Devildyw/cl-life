package top.devildyw.hmdp.utils;

import top.devildyw.hmdp.dto.UserDTO;

/**
 * @author Devil
 * @since 2023-01-19-19:27
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
