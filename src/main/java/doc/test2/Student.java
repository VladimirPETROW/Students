package doc.test2;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class Student {
    public Long id;
    public String name;
    public String fam;
    public String otch;
    @JsonFormat(pattern = "dd.MM.yyyy")
    public LocalDate bdate;
    public String team;
}
