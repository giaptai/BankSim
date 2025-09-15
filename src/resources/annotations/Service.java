package resources.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

// Giữ annotation đến lúc runtime để có thể đọc bằng Reflection
@Retention(RetentionPolicy.RUNTIME)

// Cho phép dùng annotation này trên class
@Target(ElementType.TYPE)
public @interface Service {
    String value() default "";  // tuỳ chọn: có thể truyền tên service
}
