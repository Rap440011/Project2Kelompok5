package Transaksi;

public class DetailPenjualan {

    private String idDetailPenjualan;
    private String idPenjualan;
    private String idProduk;
    private String jumlah;
    private String hargaJual;
    private String subtotal;

    public DetailPenjualan(String idDetailPenjualan, String idPenjualan,
                           String idProduk, String jumlah,
                           String hargaJual, String subtotal) {
        this.idDetailPenjualan = idDetailPenjualan;
        this.idPenjualan       = idPenjualan;
        this.idProduk          = idProduk;
        this.jumlah            = jumlah;
        this.hargaJual         = hargaJual;
        this.subtotal          = subtotal;
    }

    public String getIdDetailPenjualan()              { return idDetailPenjualan; }
    public void   setIdDetailPenjualan(String v)      { this.idDetailPenjualan = v; }

    public String getIdPenjualan()                    { return idPenjualan; }
    public void   setIdPenjualan(String v)            { this.idPenjualan = v; }

    public String getIdProduk()                       { return idProduk; }
    public void   setIdProduk(String v)               { this.idProduk = v; }

    public String getJumlah()                         { return jumlah; }
    public void   setJumlah(String v)                 { this.jumlah = v; }

    public String getHargaJual()                      { return hargaJual; }
    public void   setHargaJual(String v)              { this.hargaJual = v; }

    public String getSubtotal()                       { return subtotal; }
    public void   setSubtotal(String v)               { this.subtotal = v; }
}