package Master;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sumber data wilayah (Provinsi -> Kabupaten/Kota -> Kecamatan -> Kelurahan/Desa)
 * untuk 3 provinsi: Jawa Timur, Jawa Tengah, Jawa Barat.
 *
 * Semua data disimpan langsung di sini (tidak pakai database/SQL sama sekali).
 *
 * CATATAN: Data di bawah adalah CONTOH/STARTER SET (beberapa kabupaten/kota
 * besar saja per provinsi). Jumlah kelurahan/desa asli di 3 provinsi ini
 * mencapai ribuan, sehingga tidak realistis diketik lengkap secara manual.
 * Untuk melengkapi, tinggal tambahkan baris baru mengikuti pola yang sama
 * pada method addRows() di bawah -- ambil dari data resmi (Kemendagri/BPS)
 * lalu tempel dalam format:
 *      addRow(list, "Provinsi", "Kabupaten/Kota", "Kecamatan", "Kelurahan/Desa");
 */
public class WilayahData {

    /** Satu baris data wilayah lengkap (level kelurahan). */
    public static class Wilayah {
        public final String provinsi;
        public final String kabupaten;
        public final String kecamatan;
        public final String kelurahan;

        public Wilayah(String provinsi, String kabupaten, String kecamatan, String kelurahan) {
            this.provinsi = provinsi;
            this.kabupaten = kabupaten;
            this.kecamatan = kecamatan;
            this.kelurahan = kelurahan;
        }
    }

    private static final List<Wilayah> DATA = new ArrayList<>();

    static {
        buildData();
    }

    private static void addRow(String provinsi, String kabupaten, String kecamatan, String kelurahan) {
        DATA.add(new Wilayah(provinsi, kabupaten, kecamatan, kelurahan));
    }

    private static void buildData() {
        // ================= JAWA TIMUR =================
        addRow("Jawa Timur", "Kota Surabaya", "Genteng", "Genteng");
        addRow("Jawa Timur", "Kota Surabaya", "Genteng", "Embong Kaliasin");
        addRow("Jawa Timur", "Kota Surabaya", "Genteng", "Ketabang");
        addRow("Jawa Timur", "Kota Surabaya", "Gubeng", "Airlangga");
        addRow("Jawa Timur", "Kota Surabaya", "Gubeng", "Baratajaya");
        addRow("Jawa Timur", "Kota Surabaya", "Gubeng", "Pucang Sewu");

        addRow("Jawa Timur", "Kabupaten Sidoarjo", "Sidoarjo", "Sidokare");
        addRow("Jawa Timur", "Kabupaten Sidoarjo", "Sidoarjo", "Pucang");
        addRow("Jawa Timur", "Kabupaten Sidoarjo", "Sidoarjo", "Lemahputro");
        addRow("Jawa Timur", "Kabupaten Sidoarjo", "Waru", "Waru");
        addRow("Jawa Timur", "Kabupaten Sidoarjo", "Waru", "Tambaksawah");
        addRow("Jawa Timur", "Kabupaten Sidoarjo", "Waru", "Kureksari");

        addRow("Jawa Timur", "Kota Malang", "Klojen", "Klojen");
        addRow("Jawa Timur", "Kota Malang", "Klojen", "Kiduldalem");
        addRow("Jawa Timur", "Kota Malang", "Klojen", "Kasin");
        addRow("Jawa Timur", "Kota Malang", "Lowokwaru", "Lowokwaru");
        addRow("Jawa Timur", "Kota Malang", "Lowokwaru", "Dinoyo");
        addRow("Jawa Timur", "Kota Malang", "Lowokwaru", "Merjosari");

        // ================= JAWA TENGAH =================
        addRow("Jawa Tengah", "Kota Semarang", "Semarang Tengah", "Miroto");
        addRow("Jawa Tengah", "Kota Semarang", "Semarang Tengah", "Pindrikan Kidul");
        addRow("Jawa Tengah", "Kota Semarang", "Semarang Tengah", "Sekayu");
        addRow("Jawa Tengah", "Kota Semarang", "Candisari", "Candi");
        addRow("Jawa Tengah", "Kota Semarang", "Candisari", "Jomblang");
        addRow("Jawa Tengah", "Kota Semarang", "Candisari", "Kaliwiru");

        addRow("Jawa Tengah", "Kota Surakarta", "Laweyan", "Laweyan");
        addRow("Jawa Tengah", "Kota Surakarta", "Laweyan", "Sondakan");
        addRow("Jawa Tengah", "Kota Surakarta", "Laweyan", "Bumi");
        addRow("Jawa Tengah", "Kota Surakarta", "Banjarsari", "Nusukan");
        addRow("Jawa Tengah", "Kota Surakarta", "Banjarsari", "Manahan");
        addRow("Jawa Tengah", "Kota Surakarta", "Banjarsari", "Sumber");

        addRow("Jawa Tengah", "Kabupaten Sukoharjo", "Sukoharjo", "Sukoharjo");
        addRow("Jawa Tengah", "Kabupaten Sukoharjo", "Sukoharjo", "Jetis");
        addRow("Jawa Tengah", "Kabupaten Sukoharjo", "Sukoharjo", "Combongan");
        addRow("Jawa Tengah", "Kabupaten Sukoharjo", "Kartasura", "Kartasura");
        addRow("Jawa Tengah", "Kabupaten Sukoharjo", "Kartasura", "Pabelan");
        addRow("Jawa Tengah", "Kabupaten Sukoharjo", "Kartasura", "Ngemplak");

        // ================= JAWA BARAT =================
        addRow("Jawa Barat", "Kota Bandung", "Coblong", "Dago");
        addRow("Jawa Barat", "Kota Bandung", "Coblong", "Lebakgede");
        addRow("Jawa Barat", "Kota Bandung", "Coblong", "Sekeloa");
        addRow("Jawa Barat", "Kota Bandung", "Sumur Bandung", "Braga");
        addRow("Jawa Barat", "Kota Bandung", "Sumur Bandung", "Merdeka");
        addRow("Jawa Barat", "Kota Bandung", "Sumur Bandung", "Kebon Pisang");

        addRow("Jawa Barat", "Kota Bogor", "Bogor Tengah", "Paledang");
        addRow("Jawa Barat", "Kota Bogor", "Bogor Tengah", "Babakan Pasar");
        addRow("Jawa Barat", "Kota Bogor", "Bogor Tengah", "Panaragan");
        addRow("Jawa Barat", "Kota Bogor", "Bogor Selatan", "Batutulis");
        addRow("Jawa Barat", "Kota Bogor", "Bogor Selatan", "Empang");
        addRow("Jawa Barat", "Kota Bogor", "Bogor Selatan", "Bondongan");

        addRow("Jawa Barat", "Kabupaten Bekasi", "Cikarang Utara", "Karangasih");
        addRow("Jawa Barat", "Kabupaten Bekasi", "Cikarang Utara", "Sukaresmi");
        addRow("Jawa Barat", "Kabupaten Bekasi", "Cikarang Utara", "Cikarang Kota");
        addRow("Jawa Barat", "Kabupaten Bekasi", "Cikarang Barat", "Cibatu");
        addRow("Jawa Barat", "Kabupaten Bekasi", "Cikarang Barat", "Mekarwangi");
        addRow("Jawa Barat", "Kabupaten Bekasi", "Cikarang Barat", "Jayamukti");

        // Tambahkan baris baru di sini mengikuti pola addRow(...) di atas
        // untuk melengkapi data kabupaten/kecamatan/kelurahan lainnya.
    }

    /** Daftar semua provinsi (urut sesuai data, tanpa duplikat). */
    public static List<String> getProvinsiList() {
        Set<String> set = new LinkedHashSet<>();
        for (Wilayah w : DATA) set.add(w.provinsi);
        return new ArrayList<>(set);
    }

    /**
     * Daftar kabupaten/kota.
     * @param provinsi jika null/kosong -> tampilkan semua kabupaten dari 3 provinsi.
     *                 jika diisi -> hanya kabupaten milik provinsi tsb.
     */
    public static List<String> getKabupatenList(String provinsi) {
        Set<String> set = new LinkedHashSet<>();
        for (Wilayah w : DATA) {
            if (provinsi == null || provinsi.isEmpty() || provinsi.equals(w.provinsi)) {
                set.add(w.kabupaten);
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * Daftar kecamatan.
     * @param kabupaten jika null/kosong -> tampilkan semua kecamatan dari 3 provinsi.
     *                  jika diisi -> hanya kecamatan milik kabupaten tsb.
     */
    public static List<String> getKecamatanList(String kabupaten) {
        Set<String> set = new LinkedHashSet<>();
        for (Wilayah w : DATA) {
            if (kabupaten == null || kabupaten.isEmpty() || kabupaten.equals(w.kabupaten)) {
                set.add(w.kecamatan);
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * Daftar kelurahan/desa.
     * @param kecamatan jika null/kosong -> tampilkan semua kelurahan dari 3 provinsi.
     *                  jika diisi -> hanya kelurahan milik kecamatan tsb.
     */
    public static List<String> getKelurahanList(String kecamatan) {
        Set<String> set = new LinkedHashSet<>();
        for (Wilayah w : DATA) {
            if (kecamatan == null || kecamatan.isEmpty() || kecamatan.equals(w.kecamatan)) {
                set.add(w.kelurahan);
            }
        }
        return new ArrayList<>(set);
    }
}
