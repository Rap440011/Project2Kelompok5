package Transaksi;

public class TransaksiSetorLimbah {

    private String idTransaksi;
    private String idNasabah;
    private String namaNasabah;
    private String idKaryawan;
    private String tanggal;
    private String total;

    public TransaksiSetorLimbah(String idTransaksi, String idNasabah, String namaNasabah,
                                 String idKaryawan, String tanggal, String total) {
        this.idTransaksi = idTransaksi;
        this.idNasabah = idNasabah;
        this.namaNasabah = namaNasabah;
        this.idKaryawan = idKaryawan;
        this.tanggal = tanggal;
        this.total = total;
    }

    public String getIdTransaksi() { return idTransaksi; }
    public void setIdTransaksi(String idTransaksi) { this.idTransaksi = idTransaksi; }

    public String getIdNasabah() { return idNasabah; }
    public void setIdNasabah(String idNasabah) { this.idNasabah = idNasabah; }

    public String getNamaNasabah() { return namaNasabah; }
    public void setNamaNasabah(String namaNasabah) { this.namaNasabah = namaNasabah; }

    public String getIdKaryawan() { return idKaryawan; }
    public void setIdKaryawan(String idKaryawan) { this.idKaryawan = idKaryawan; }

    public String getTanggal() { return tanggal; }
    public void setTanggal(String tanggal) { this.tanggal = tanggal; }

    public String getTotal() { return total; }
    public void setTotal(String total) { this.total = total; }
}
