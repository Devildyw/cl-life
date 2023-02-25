package top.devildyw.hmdp.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.devildyw.hmdp.dto.Result;

import java.util.HashMap;

/**
 * @author Devil
 * @since 2023-02-25-19:56
 */

@Slf4j
@RestControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail("服务器异常");
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result handleValidException(MethodArgumentNotValidException e){

        BindingResult bindingResult = e.getBindingResult(); //获取校验结果

        HashMap<String,String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((item)->{
            errorMap.put(item.getField(),item.getDefaultMessage());
        });

        log.warn("数据校验出现问题{}",errorMap);
        return Result.exception(errorMap);
    }
}
