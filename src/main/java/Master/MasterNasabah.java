package Master;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MasterNasabah {

    private StringProperty idNasabah;
    private StringProperty namaNasabah;
    private StringProperty noHp;
    private StringProperty rt;
    private StringProperty rw;
    private StringProperty kelurahan;
    private StringProperty kecamatan;
    private StringProperty kabupaten;
    private StringProperty provinsi;
    private StringProperty noRekening;
    private StringProperty saldo;
    private StringProperty bank;

    public MasterNasabah(String idNasabah, String namaNasabah, String noHp,
                    String rt, String rw, String kelurahan, String kecamatan,
                    String kabupaten, String provinsi, String noRekening, String saldo, String bank) {
        this.idNasabah   = new SimpleStringProperty(idNasabah);
        this.namaNasabah = new SimpleStringProperty(namaNasabah);
        this.noHp        = new SimpleStringProperty(noHp);
        this.rt          = new SimpleStringProperty(rt);
        this.rw          = new SimpleStringProperty(rw);
        this.kelurahan   = new SimpleStringProperty(kelurahan);
        this.kecamatan   = new SimpleStringProperty(kecamatan);
        this.kabupaten   = new SimpleStringProperty(kabupaten);
        this.provinsi    = new SimpleStringProperty(provinsi);
        this.noRekening  = new SimpleStringProperty(noRekening);
        this.saldo       = new SimpleStringProperty(saldo);
        this.bank        = new SimpleStringProperty(bank);
    }

    public String getIdNasabah()   { return idNasabah.get(); }
    public String getNamaNasabah() { return namaNasabah.get(); }
    public String getNoHp()        { return noHp.get(); }
    public String getRt()          { return rt.get(); }
    public String getRw()          { return rw.get(); }
    public String getKelurahan()   { return kelurahan.get(); }
    public String getKecamatan()   { return kecamatan.get(); }
    public String getKabupaten()   { return kabupaten.get(); }
    public String getProvinsi()    { return provinsi.get(); }
    public String getNoRekening()  { return noRekening.get(); }
    public String getSaldo()       { return saldo.get(); }
    public String getBank()        { return bank.get();}

    public StringProperty idNasabahProperty()   { return idNasabah; }
    public StringProperty namaNasabahProperty() { return namaNasabah; }
    public StringProperty noHpProperty()        { return noHp; }
    public StringProperty rtProperty()          { return rt; }
    public StringProperty rwProperty()          { return rw; }
    public StringProperty kelurahanProperty()   { return kelurahan; }
    public StringProperty kecamatanProperty()   { return kecamatan; }
    public StringProperty kabupatenProperty()   { return kabupaten; }
    public StringProperty provinsiProperty()    { return provinsi; }
    public StringProperty noRekeningProperty()  { return noRekening; }
    public StringProperty saldoProperty()       { return saldo; }
    public StringProperty bankProperty()        { return bank; }
}