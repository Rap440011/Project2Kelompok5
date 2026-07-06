package Transaksi;

public class TransaksiPenjualan {

    private String idPenjualan;
    private String idKaryawan;
    private String tanggal;
    private String total;

    public TransaksiPenjualan(String idPenjualan, String idKaryawan,
                              String tanggal, String total) {
        this.idPenjualan = idPenjualan;
        this.idKaryawan  = idKaryawan;
        this.tanggal     = tanggal;
        this.total       = total;
    }

    public String getIdPenjualan()               { return idPenjualan; }
    public void   setIdPenjualan(String v)       { this.idPenjualan = v; }

    public String getIdKaryawan()                { return idKaryawan; }
    public void   setIdKaryawan(String v)        { this.idKaryawan = v; }

    public String getTanggal()                   { return tanggal; }
    public void   setTanggal(String v)           { this.tanggal = v; }

    public String getTotal()                     { return total; }
    public void   setTotal(String v)             { this.total = v; }
}