package top.devildyw.cl_dianping.common.utils;


import top.devildyw.cl_dianping.common.DTO.UserDTO;

/**
 * @author Devil
 * @since 2023-01-19-19:27
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user) {
        tl.set(user);
    }

    public static UserDTO getUser() {
        return tl.get();
    }

    public static void removeUser() {
        tl.remove();
    }
}
