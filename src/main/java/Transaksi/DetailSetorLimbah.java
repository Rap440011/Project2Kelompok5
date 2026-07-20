package Transaksi;

/**
 * Model baris detail pada transaksi Setor Limbah.
 * Mengikuti struktur tabel dtl_tr_Setor_Limbah, dengan PRIMARY KEY
 * komposit (ID_Setor, ID_Limbah) — sudah tidak ada lagi ID_Detail_Setor.
 */
public class DetailSetorLimbah {

    private String idSetor;
    private String idLimbah;
    private String jenis;       // Nama limbah (ditampilkan di tabel)
    private String jumlah;
    private String satuan;
    private String keterangan;
    private String subTotal;

    public DetailSetorLimbah(String idSetor, String idLimbah, String jenis,
                             String jumlah, String satuan, String keterangan,
                             String subTotal) {
        this.idSetor = idSetor;
        this.idLimbah = idLimbah;
        this.jenis = jenis;
        this.jumlah = jumlah;
        this.satuan = satuan;
        this.keterangan = keterangan;
        this.subTotal = subTotal;
    }

    public String getIdSetor() {
        return idSetor;
    }

    public void setIdSetor(String idSetor) {
        this.idSetor = idSetor;
    }

    public String getIdLimbah() {
        return idLimbah;
    }

    public void setIdLimbah(String idLimbah) {
        this.idLimbah = idLimbah;
    }

    public String getJenis() {
        return jenis;
    }

    public void setJenis(String jenis) {
        this.jenis = jenis;
    }

    public String getJumlah() {
        return jumlah;
    }

    public void setJumlah(String jumlah) {
        this.jumlah = jumlah;
    }

    public String getSatuan() {
        return satuan;
    }

    public void setSatuan(String satuan) {
        this.satuan = satuan;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    public String getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(String subTotal) {
        this.subTotal = subTotal;
    }
}