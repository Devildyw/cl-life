package top.devildyw.cl_dianping.common.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Devil
 * @since 2023-02-25-21:15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillVoucherMQDTO {
    private Long userId;

    private Long voucherId;

    private Long orderId;
}
