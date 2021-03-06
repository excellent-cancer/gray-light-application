package gray.light.owner.business;

import gray.light.support.error.NormalizingFormException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import perishing.constraint.treasure.chest.ResourceTreasureChest;
import perishing.constraint.treasure.chest.StringTreasureChest;

import static org.springframework.util.StringUtils.isEmpty;
import static org.springframework.util.StringUtils.trimAllWhitespace;

/**
 * 创建owner-project的请求表单
 *
 * @author XyParaCrim
 */
@Data
public class OwnerProjectFo {

    private String name;

    private String description;

    private String homePage;

    private Long ownerId;

    /**
     * 对请求表单进行标准化和检查
     *
     * @throws NormalizingFormException 表单属性不合格的时候
     */
    public void normalize() {
        this.name = trimAllWhitespace(name);
        this.homePage = trimAllWhitespace(homePage);
        this.description = trimAllWhitespace(StringTreasureChest.normalize(description));

        if (isEmpty(name)) {
            NormalizingFormException.emptyProperty("name");
        }

        if (isEmpty(homePage) && !ResourceTreasureChest.isUrl(homePage)) {
            NormalizingFormException.emptyProperty("homePage");
        }

        if (isEmpty(ownerId)) {
            NormalizingFormException.emptyProperty("ownerId");
        }
    }

}
