package Master;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MasterLimbah {

    private StringProperty idLimbah;
    private StringProperty jenisLimbah;
    private StringProperty satuan;
    private StringProperty harga;
    private StringProperty keterangan;

    public MasterLimbah(String idLimbah, String jenisLimbah, String satuan,
                        String harga, String keterangan) {
        this.idLimbah    = new SimpleStringProperty(idLimbah);
        this.jenisLimbah = new SimpleStringProperty(jenisLimbah);
        this.satuan      = new SimpleStringProperty(satuan);
        this.harga       = new SimpleStringProperty(harga);
        this.keterangan  = new SimpleStringProperty(keterangan);
    }

    public String getIdLimbah()    { return idLimbah.get(); }
    public String getJenisLimbah() { return jenisLimbah.get(); }
    public String getSatuan()      { return satuan.get(); }
    public String getHarga()       { return harga.get(); }
    public String getKeterangan()  { return keterangan.get(); }

    public StringProperty idLimbahProperty()    { return idLimbah; }
    public StringProperty jenisLimbahProperty() { return jenisLimbah; }
    public StringProperty satuanProperty()      { return satuan; }
    public StringProperty hargaProperty()       { return harga; }
    public StringProperty keteranganProperty()  { return keterangan; }
}