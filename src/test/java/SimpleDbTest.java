import com.ll.Article;
import com.ll.SimpleDb;
import com.ll.Sql;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@TestMethodOrder(MethodOrderer.MethodName.class)
    public class SimpleDbTest {
        private static SimpleDb simpleDb;

        @BeforeAll
        public static void beforeAll() {
            simpleDb = new SimpleDb("localhost:3306", "root", "lldj123414", "simpleDb__test");
            simpleDb.setDevMode(true);

            createArticleTable();
        }

        @BeforeEach
        public void beforeEach() {
            truncateArticleTable();
            makeArticleTestData();
        }

        private static void createArticleTable() {
            simpleDb.run("DROP TABLE IF EXISTS article");

            simpleDb.run("""
                CREATE TABLE article (
                    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY(id),
                    createdDate DATETIME NOT NULL,
                    modifiedDate DATETIME NOT NULL,
                    title VARCHAR(100) NOT NULL,
                    `body` TEXT NOT NULL,
                    isBlind BIT(1) NOT NULL DEFAULT 0
                )
                """);
        }

        private void makeArticleTestData() {
            IntStream.rangeClosed(1, 6).forEach(no -> {
                boolean isBlind = no > 3;
                String title = "제목%d".formatted(no);
                String body = "내용%d".formatted(no);

                simpleDb.run("""
                    INSERT INTO article
                    SET createdDate = NOW(),
                    modifiedDate = NOW(),
                    title = ?,
                    `body` = ?,
                    isBlind = ?
                    """, title, body, isBlind);
            });
        }

        private void truncateArticleTable() {
            simpleDb.run("TRUNCATE article");
        }

        @Test
        @DisplayName("insert")
        public void t001() {
            Sql sql = simpleDb.genSql();
            sql.append("INSERT INTO article")
                    .append("SET createdDate = NOW()")
                    .append(", modifiedDate = NOW()")
                    .append(", title = ?", "제목 new")
                    .append(", body = ?", "내용 new");

            long newId = sql.insert(); // AUTO_INCREMENT 에 의해서 생성된 주키 리턴

            assertThat(newId).isGreaterThan(0);
        }

    @Test
    @DisplayName("update")
    public void t002() {
        Sql sql = simpleDb.genSql();

        // id가 0, 1, 2, 3인 글 수정
        // id가 0인 글은 없으니, 실제로는 3개의 글이 삭제됨

        /*
        == rawSql ==
        UPDATE article
        SET title = '제목 new'
        WHERE id IN ('0', '1', '2', '3')
        */
        sql.append("UPDATE article")
                .append("SET title = ?", "제목 new")
                .append("WHERE id IN (?, ?, ?, ?)", 0, 1, 2, 3);

        // 수정된 row 개수
        long affectedRowsCount = sql.update();

        assertThat(affectedRowsCount).isEqualTo(3);
    }

    @Test
    @DisplayName("delete")
    public void t003() {
        Sql sql = simpleDb.genSql();

        // id가 0, 1, 3인 글 삭제
        // id가 0인 글은 없으니, 실제로는 2개의 글이 삭제됨
        /*
        == rawSql ==
        DELETE FROM article
        WHERE id IN ('0', '1', '3')
        */
        sql.append("DELETE")
                .append("FROM article")
                .append("WHERE id IN (?, ?, ?)", 0, 1, 3);

        // 삭제된 row 개수
        long affectedRowsCount = sql.delete();

        assertThat(affectedRowsCount).isEqualTo(2);
    }

    @Test
    @DisplayName("selectRow, Article")
    public void t016() {
        Sql sql = simpleDb.genSql();
        /*
        == rawSql ==
        SELECT *
        FROM article
        WHERE id = 1
        */
        sql.append("SELECT * FROM article WHERE id = 1");
        Article article = sql.selectRow(Article.class);

        Long id = 1L;

        assertThat(article.getId()).isEqualTo(id);
        assertThat(article.getTitle()).isEqualTo("제목%d".formatted(id));
        assertThat(article.getBody()).isEqualTo("내용%d".formatted(id));
        assertThat(article.getCreatedDate()).isInstanceOf(LocalDateTime.class);
        assertThat(article.getCreatedDate()).isNotNull();
        assertThat(article.getModifiedDate()).isInstanceOf(LocalDateTime.class);
        assertThat(article.getModifiedDate()).isNotNull();
        assertThat(article.isBlind()).isEqualTo(false);
    }


}