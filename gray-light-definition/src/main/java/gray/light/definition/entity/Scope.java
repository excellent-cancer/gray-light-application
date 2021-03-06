package gray.light.definition.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 记录该应用所有范围，例如文档，笔记等
 *
 * @author XyParaCrim
 */
@Data
@AllArgsConstructor
public class Scope {

    private String name;

    private String description;

    public final static Scope WORKS = new Scope("works", "");

    public final static Scope DOCUMENT = new Scope("document", "");

    public final static Scope NOTE = new Scope("note", "");

    public final static Scope BLOG = new Scope("blog", "");

    public final static List<Scope> SUPPORTED = List.of(WORKS, DOCUMENT, NOTE, BLOG);

}
