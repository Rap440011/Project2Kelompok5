package Transaksi;

public class PenarikanSaldo {

    private String idNasabah;
    private String namaNasabah;
    private String noHp;
    private String saldo;      // disimpan dalam bentuk angka mentah (tanpa "Rp"), misal "150000"

    public PenarikanSaldo(String idNasabah, String namaNasabah, String noHp, String saldo) {
        this.idNasabah   = idNasabah;
        this.namaNasabah = namaNasabah;
        this.noHp        = noHp;
        this.saldo       = saldo;
    }

    public String getIdNasabah() { return idNasabah; }
    public void setIdNasabah(String idNasabah) { this.idNasabah = idNasabah; }

    public String getNamaNasabah() { return namaNasabah; }
    public void setNamaNasabah(String namaNasabah) { this.namaNasabah = namaNasabah; }

    public String getNoHp() { return noHp; }
    public void setNoHp(String noHp) { this.noHp = noHp; }

    public String getSaldo() { return saldo; }
    public void setSaldo(String saldo) { this.saldo = saldo; }
}