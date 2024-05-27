package kitchenpos.products.infra;

import kitchenpos.common.infra.PurgomalumClient;
import kitchenpos.products.application.ProfanityDetectionClient;
import org.springframework.stereotype.Component;

@Component
public class ProductProfanityDetectionClient implements ProfanityDetectionClient {
    private final PurgomalumClient purgomalumClient;

    public ProductProfanityDetectionClient(PurgomalumClient purgomalumClient) {
        this.purgomalumClient = purgomalumClient;
    }

    @Override
    public boolean containsProfanity(String text) {
        return purgomalumClient.containsProfanity(text);
    }
}
