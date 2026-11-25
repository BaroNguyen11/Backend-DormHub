package com.dormhub.specification;

import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.jpa.domain.Specification;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/** Text filtering in SQL before pagination. LEFT joins preserve optional relationships. */
public final class SearchSpecifications {
    private SearchSpecifications() {}
    public static final String ACCENTS = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";
    public static final String ASCII = "aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyd";

    public static String normalize(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (text.length() > 200) throw new AppException(ErrorCode.INVALID_REQUEST, "Từ khóa tối đa 200 ký tự");
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replaceAll("\\s+", " ");
    }

    public static String pattern(String value) {
        return "%" + normalize(value).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private static Path<?> path(From<?, ?> root, String field, java.util.Map<String, From<?, ?>> joins) {
        String[] parts = field.split("\\.");
        From<?, ?> parent = root;
        String prefix = "";
        for (int i = 0; i < parts.length - 1; i++) {
            prefix += "." + parts[i];
            From<?, ?> existing = joins.get(prefix);
            if (existing == null) {
                existing = parent.join(parts[i], JoinType.LEFT);
                joins.put(prefix, existing);
            }
            parent = existing;
        }
        return parent.get(parts[parts.length - 1]);
    }

    public static <T> Specification<T> text(String search, String... fields) {
        String value = normalize(search);
        return (root, query, cb) -> {
            if (value.isEmpty()) return cb.conjunction();
            var joins = new HashMap<String, From<?, ?>>();
            var tokens = new ArrayList<jakarta.persistence.criteria.Predicate>();
            for (String token : value.split(" ")) {
                var alternatives = new ArrayList<jakarta.persistence.criteria.Predicate>();
                for (String field : fields) {
                    var expression = ((JpaExpression<?>) path(root, field, joins)).cast(String.class);
                    var folded = cb.function("translate", String.class, cb.lower(expression),
                            cb.literal(ACCENTS), cb.literal(ASCII));
                    alternatives.add(cb.like(folded, pattern(token), '!'));
                }
                tokens.add(cb.or(alternatives.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }
            return cb.and(tokens.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public static <T> Specification<T> equal(String field, Object value) {
        return (root, query, cb) -> value == null ? cb.conjunction()
                : cb.equal(path(root, field, new HashMap<>()), value);
    }
}
