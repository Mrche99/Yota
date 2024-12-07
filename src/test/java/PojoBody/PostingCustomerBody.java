package PojoBody;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter@Setter
@AllArgsConstructor
public class PostingCustomerBody {
    String name;
    String phone;
    Map<String,String> additionalParameters;
}
