package Master;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MasterLimbah {

    private StringProperty idLimbah;
    private StringProperty namaLimbah;
    private StringProperty jenisLimbah;
    private StringProperty satuan;
    private StringProperty harga;
    private StringProperty keterangan;
    private StringProperty jumlah;

    public MasterLimbah(String idLimbah, String namaLimbah, String jenisLimbah, String satuan,
                        String harga, String keterangan, String jumlah) {
        this.idLimbah    = new SimpleStringProperty(idLimbah);
        this.namaLimbah  = new SimpleStringProperty(namaLimbah);
        this.jenisLimbah = new SimpleStringProperty(jenisLimbah);
        this.satuan      = new SimpleStringProperty(satuan);
        this.harga       = new SimpleStringProperty(harga);
        this.keterangan  = new SimpleStringProperty(keterangan);
        this.jumlah      = new SimpleStringProperty(jumlah == null ? "0" : jumlah);
    }

    public String getIdLimbah()    { return idLimbah.get(); }
    public String getNamaLimbah()  { return namaLimbah.get(); }
    public String getJenisLimbah() { return jenisLimbah.get(); }
    public String getSatuan()      { return satuan.get(); }
    public String getHarga()       { return harga.get(); }
    public String getKeterangan()  { return keterangan.get(); }
    public String getJumlah()      { return jumlah.get(); }

    public void setIdLimbah(String v)    { idLimbah.set(v); }
    public void setNamaLimbah(String v)  { namaLimbah.set(v); }
    public void setJenisLimbah(String v) { jenisLimbah.set(v); }
    public void setSatuan(String v)      { satuan.set(v); }
    public void setHarga(String v)       { harga.set(v); }
    public void setKeterangan(String v)  { keterangan.set(v); }
    public void setJumlah(String v)      { jumlah.set(v); }

    public StringProperty idLimbahProperty()    { return idLimbah; }
    public StringProperty namaLimbahProperty()  { return namaLimbah; }
    public StringProperty jenisLimbahProperty() { return jenisLimbah; }
    public StringProperty satuanProperty()      { return satuan; }
    public StringProperty hargaProperty()       { return harga; }
    public StringProperty keteranganProperty()  { return keterangan; }
    public StringProperty jumlahProperty()      { return jumlah; }
}