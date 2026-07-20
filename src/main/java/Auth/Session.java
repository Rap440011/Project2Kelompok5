package Auth;

/**
 * Menyimpan data karyawan yang sedang login, supaya bisa diakses
 * dari controller mana pun tanpa perlu passing manual.
 */
public class Session {
    private static String idKaryawanLogin;

    private Session() {}

    public static void setIdKaryawanLogin(String idKaryawan) {
        idKaryawanLogin = idKaryawan;
    }

    public static String getIdKaryawanLogin() {
        return idKaryawanLogin;
    }

    public static void clear() {
        idKaryawanLogin = null;
    }
}