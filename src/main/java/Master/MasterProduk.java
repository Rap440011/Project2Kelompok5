package Master;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MasterProduk {

    private StringProperty idProduk;
    private StringProperty namaProduk;
    private StringProperty stok;
    private StringProperty hargaJual;
    private StringProperty satuan;
    private StringProperty keterangan;

    public MasterProduk(String idProduk, String namaProduk, String stok,
                   String hargaJual, String satuan, String keterangan) {
        this.idProduk   = new SimpleStringProperty(idProduk);
        this.namaProduk = new SimpleStringProperty(namaProduk);
        this.stok       = new SimpleStringProperty(stok);
        this.hargaJual  = new SimpleStringProperty(hargaJual);
        this.satuan     = new SimpleStringProperty(satuan);
        this.keterangan = new SimpleStringProperty(keterangan);
    }

    public String getIdProduk()   { return idProduk.get(); }
    public String getNamaProduk() { return namaProduk.get(); }
    public String getStok()       { return stok.get(); }
    public String getHargaJual()  { return hargaJual.get(); }
    public String getSatuan()     { return satuan.get(); }
    public String getKeterangan() { return keterangan.get(); }

    public StringProperty idProdukProperty()   { return idProduk; }
    public StringProperty namaProdukProperty() { return namaProduk; }
    public StringProperty stokProperty()       { return stok; }
    public StringProperty hargaJualProperty()  { return hargaJual; }
    public StringProperty satuanProperty()     { return satuan; }
    public StringProperty keteranganProperty() { return keterangan; }
}