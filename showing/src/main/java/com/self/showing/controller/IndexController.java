package com.self.showing.controller;

import com.self.showing.entity.MovieList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * created by Bonnie on 2021/4/25
 */
@Controller
public class IndexController {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @RequestMapping("/index")  //MovieList
    public String results(Model model) throws IOException {
        String sql = "select m.name, round(t.avg_score, 0) avg_score from top10Results t left join movies m on t.movieid = m.id;";
        List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql);
        List<MovieList> res = new ArrayList<>();
        for( Map<String, Object> t: maps ) {
            System.out.println("name-" + t.get("name").toString() + " score-" + t.get("avg_score").toString());
            MovieList tmp = new MovieList();
            tmp.setMovie_id(t.get("name").toString());
            tmp.setScore(Integer.parseInt(t.get("avg_score").toString()));
            res.add(tmp);
        }
        model.addAttribute("list", res);
        return "index";
    }

}
