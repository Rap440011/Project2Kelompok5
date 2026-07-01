package Transaksi;

public class DetailSetorLimbah {

    private String idDetail;
    private String idTransaksi;
    private String jenis;
    private String jumlah;
    private String satuan;
    private String keterangan;
    private String subTotal;

    public DetailSetorLimbah(String idDetail, String idTransaksi, String jenis, String jumlah,
                              String satuan, String keterangan, String subTotal) {
        this.idDetail = idDetail;
        this.idTransaksi = idTransaksi;
        this.jenis = jenis;
        this.jumlah = jumlah;
        this.satuan = satuan;
        this.keterangan = keterangan;
        this.subTotal = subTotal;
    }

    public String getIdDetail() { return idDetail; }
    public void setIdDetail(String idDetail) { this.idDetail = idDetail; }

    public String getIdTransaksi() { return idTransaksi; }
    public void setIdTransaksi(String idTransaksi) { this.idTransaksi = idTransaksi; }

    public String getJenis() { return jenis; }
    public void setJenis(String jenis) { this.jenis = jenis; }

    public String getJumlah() { return jumlah; }
    public void setJumlah(String jumlah) { this.jumlah = jumlah; }

    public String getSatuan() { return satuan; }
    public void setSatuan(String satuan) { this.satuan = satuan; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    public String getSubTotal() { return subTotal; }
    public void setSubTotal(String subTotal) { this.subTotal = subTotal; }
}
