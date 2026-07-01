package Master;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MasterKaryawan {

    private StringProperty idKaryawan;
    private StringProperty namaKaryawan;
    private StringProperty username;
    private StringProperty password;
    private StringProperty rt;
    private StringProperty rw;
    private StringProperty kelurahan;
    private StringProperty kecamatan;
    private StringProperty kabupaten;
    private StringProperty provinsi;
    private StringProperty noHp;
    private StringProperty jabatan;
    private StringProperty status;
    private StringProperty jenisKelamin;

    public MasterKaryawan(String idKaryawan, String namaKaryawan, String username,
                          String password, String rt, String rw,
                          String kelurahan, String kecamatan, String kabupaten,
                          String provinsi, String noHp, String jabatan, String status,
                          String jenisKelamin) {
        this.idKaryawan   = new SimpleStringProperty(idKaryawan);
        this.namaKaryawan = new SimpleStringProperty(namaKaryawan);
        this.username     = new SimpleStringProperty(username);
        this.password     = new SimpleStringProperty(password);
        this.rt           = new SimpleStringProperty(rt);
        this.rw           = new SimpleStringProperty(rw);
        this.kelurahan    = new SimpleStringProperty(kelurahan);
        this.kecamatan    = new SimpleStringProperty(kecamatan);
        this.kabupaten    = new SimpleStringProperty(kabupaten);
        this.provinsi     = new SimpleStringProperty(provinsi);
        this.noHp         = new SimpleStringProperty(noHp);
        this.jabatan      = new SimpleStringProperty(jabatan);
        this.status       = new SimpleStringProperty(status);
        this.jenisKelamin = new SimpleStringProperty(jenisKelamin);
    }

    public String getIdKaryawan()   { return idKaryawan.get(); }
    public String getNamaKaryawan() { return namaKaryawan.get(); }
    public String getUsername()     { return username.get(); }
    public String getPassword()     { return password.get(); }
    public String getRt()           { return rt.get(); }
    public String getRw()           { return rw.get(); }
    public String getKelurahan()    { return kelurahan.get(); }
    public String getKecamatan()    { return kecamatan.get(); }
    public String getKabupaten()    { return kabupaten.get(); }
    public String getProvinsi()     { return provinsi.get(); }
    public String getNoHp()         { return noHp.get(); }
    public String getJabatan()      { return jabatan.get(); }
    public String getStatus()       { return status.get(); }
    public String getJenisKelamin() { return jenisKelamin.get(); }

    public StringProperty idKaryawanProperty()   { return idKaryawan; }
    public StringProperty namaKaryawanProperty() { return namaKaryawan; }
    public StringProperty usernameProperty()     { return username; }
    public StringProperty passwordProperty()     { return password; }
    public StringProperty rtProperty()           { return rt; }
    public StringProperty rwProperty()           { return rw; }
    public StringProperty kelurahanProperty()    { return kelurahan; }
    public StringProperty kecamatanProperty()    { return kecamatan; }
    public StringProperty kabupatenProperty()    { return kabupaten; }
    public StringProperty provinsiProperty()     { return provinsi; }
    public StringProperty noHpProperty()         { return noHp; }
    public StringProperty jabatanProperty()      { return jabatan; }
    public StringProperty statusProperty()       { return status; }
    public StringProperty jenisKelaminProperty() { return jenisKelamin; }
}