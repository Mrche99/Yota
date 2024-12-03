import lombok.Getter;

@Getter
public class CustomerInfo {
    private String idCustomer;
    private Long phone;

    public CustomerInfo(String s, Long aLong) {
        this.idCustomer =s;
        this.phone = aLong;
    }
}
