package top.devildyw.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
