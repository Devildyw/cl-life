package top.devildyw.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Devil
 * @since 2023-01-11-15:27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
