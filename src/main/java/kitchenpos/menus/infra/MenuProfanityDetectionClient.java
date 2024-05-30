package kitchenpos.menus.infra;

import kitchenpos.common.infra.PurgomalumClient;
import kitchenpos.menus.application.ProfanityDetectionClient;
import org.springframework.stereotype.Component;

@Component
public class MenuProfanityDetectionClient implements ProfanityDetectionClient {
    private final PurgomalumClient purgomalumClient;

    public MenuProfanityDetectionClient(PurgomalumClient purgomalumClient) {
        this.purgomalumClient = purgomalumClient;
    }

    @Override
    public boolean containsProfanity(String text) {
        return purgomalumClient.containsProfanity(text);
    }
}
