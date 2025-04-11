package ru.rdc.FomsService.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.rdc.FomsService.dto.Item;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
//Репозиторий, для получения данных из МИС
public class MisRepository {
    private final JdbcTemplate jdbcTemplate;

    public MisRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Item> findErrorsByDateRange(String startDate, String endDate) {
        String sql = """
            select distinct
            '111' as codeusl
            ,'Данные из БД' as errorcode
            ,p.lastname as fam
            ,p.firstname as im
            ,p.secondname as ot
            ,TO_CHAR(p.birthdate, 'dd.mm.yyyy') as birthdate
            ,pol.ser as spolis
            ,pol.code as npolis
            ,'Данные из БД' as refreason
            ,'Данные из БД' as scom
            ,'МКБ' as diagnosis
            ,(select d.text from docdep dd, dep d where v.doctorid = dd.keyid and dd.depid = d.keyid) as namemo
            ,'Доктор' as doccode
            ,0.0 as sumvusl
            ,0.0 as sanksum
            ,'H' as nhistory
            ,TO_CHAR(v.dat, 'dd.mm.yyyy') as datein
            ,TO_CHAR(v.dat, 'dd.mm.yyyy') as dateout
            ,'IDSTRAX' as idstrax
            ,(select decode(c.stext, 'T05', 1, 0) from company c where a.companyid = c.keyid) as inogor
            ,'SMO' as smo
            from
            visit v,
            patserv ps,
            invoice i,
            police pol,
            agr a,
            srvdep sd,
            patient p
            where
            ps.visitid = v.keyid
            and i.patservid = ps.keyid
            and ps.policeid = pol.keyid
            and ps.srvdepid = sd.keyid
            and v.patientid = p.keyid
            and v.agrid = a.keyid
            AND v.vistype BETWEEN 1 AND 95
            and sd.service_group_id in (17, 18, 19, 20, 21, 22, 23, 86, 87, 88, 96, 105, 113)
            and a.keyid in (4,5,7)
            and length(pol.code) = 16
            AND TRUNC(v.dat) BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')
        """;

        return jdbcTemplate.query(sql, new Object[]{startDate, endDate}, new OracleModelRowMapper());
    }

    private static class OracleModelRowMapper implements RowMapper<Item> {
        @Override
        public Item mapRow(ResultSet rs, int rowNum) throws SQLException {
            Item item = new Item();

            item.setS_com(rs.getString("scom"));
            item.setSpolis(rs.getString("spolis"));
            item.setNpolis(rs.getString("npolis"));
            item.setFam(rs.getString("fam").toUpperCase());
            item.setIm(rs.getString("im").toUpperCase());
            item.setOt(rs.getString("ot") != null ? rs.getString("ot").toUpperCase() : null);
            item.setBirthDate(rs.getString("birthdate"));
            item.setDate_in(LocalDate.parse(rs.getString("datein"), DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            item.setDate_out(LocalDate.parse(rs.getString("dateout"), DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            item.setRefreason(rs.getString("refreason"));
            item.setCodeUsl(rs.getString("codeusl"));
            item.setSum(rs.getDouble("sumvusl"));
            item.setSumv(rs.getDouble("sumvusl"));
            item.setDs1(rs.getString("diagnosis"));
            item.setNameMO(rs.getString("namemo"));
            item.setIddokt(rs.getString("doccode"));
            item.setNhistory(rs.getString("nhistory"));
            item.setSchet("H");
            item.setIdstrax("idstrax");
            item.setIdserv("idstrax");
            return item;
        }
    }
}