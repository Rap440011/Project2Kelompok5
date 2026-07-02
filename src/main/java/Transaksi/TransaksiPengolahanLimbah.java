package Transaksi;

public class TransaksiPengolahanLimbah {

    private String idPengolahan;
    private String idProduk;
    private String tanggal;
    private String jenisProduk;
    private String kuantitasHasil;
    private String satuan;
    private String keterangan;

    public TransaksiPengolahanLimbah(String idPengolahan, String idProduk, String tanggal,
                                      String jenisProduk, String kuantitasHasil,
                                      String satuan, String keterangan) {
        this.idPengolahan = idPengolahan;
        this.idProduk = idProduk;
        this.tanggal = tanggal;
        this.jenisProduk = jenisProduk;
        this.kuantitasHasil = kuantitasHasil;
        this.satuan = satuan;
        this.keterangan = keterangan;
    }

    public String getIdPengolahan() { return idPengolahan; }
    public void setIdPengolahan(String idPengolahan) { this.idPengolahan = idPengolahan; }

    public String getIdProduk() { return idProduk; }
    public void setIdProduk(String idProduk) { this.idProduk = idProduk; }

    public String getTanggal() { return tanggal; }
    public void setTanggal(String tanggal) { this.tanggal = tanggal; }

    public String getJenisProduk() { return jenisProduk; }
    public void setJenisProduk(String jenisProduk) { this.jenisProduk = jenisProduk; }

    public String getKuantitasHasil() { return kuantitasHasil; }
    public void setKuantitasHasil(String kuantitasHasil) { this.kuantitasHasil = kuantitasHasil; }

    public String getSatuan() { return satuan; }
    public void setSatuan(String satuan) { this.satuan = satuan; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
}