Задание 1.

CREATE PROCEDURE times(seconds INT)
BEGIN
    DECLARE days INT default 0;
    DECLARE hours INT default 0;
    DECLARE minutes INT default 0;

    WHILE seconds >= 84600 DO
    SET days = seconds / 84600;
    SET seconds = seconds % 84600;
    END WHILE;

    WHILE seconds >= 3600 DO
    SET hours = seconds / 3600;
    SET seconds = seconds % 3600;
    END WHILE;

    WHILE seconds >= 60 DO
    SET minutes = seconds / 60;
    SET seconds = seconds % 60;
    END WHILE;

SELECT days, hours, minutes, seconds;
END 

CALL times(123456);



Задание 2.

CREATE PROCEDURE get_even(`start` INT, `end` INT)
BEGIN
	DECLARE i INT DEFAULT `start`;
    DECLARE res_str TEXT DEFAULT NULL;
    WHILE  i<=`end` DO
        IF i%2 = 0 THEN
			IF res_str IS NULL THEN
				SET res_str = concat(i);
			ELSE
				SET res_str = concat(res_str, ', ', i);
			END IF;
		END IF;
        SET i = i + 1;
    END WHILE;
	SELECT res_str;
END //
DELIMITER ;

CALL get_even(1, 10);
CALL second_counter(123456);  

Дополнительное задание
1. Создать процедуру, которая решает следующую задачу
Выбрать для одного пользователя 5 пользователей в случайной комбинации, которые удовлетворяют хотя бы одному критерию:
а) из одного города
б) состоят в одной группе
в) друзья друзей	

DROP PROCEDURE IF EXISTS 5_users;
DELIMITER //
CREATE PROCEDURE 5_users
(
	IN id_user_find INT
)
BEGIN
	
    SELECT t.id
    FROM
    (
		SELECT id
		FROM users u
		INNER JOIN profiles p
		ON u.id = p.user_id
		AND u.id <> id_user_find
		AND (
			SELECT p1.hometown
			FROM users u1
			INNER JOIN profiles p1
			ON u1.id = p1.user_id
			AND u1.id = id_user_find
		) = p.hometown
		UNION    
		SELECT DISTINCT u.id 
		FROM users u
		INNER JOIN users_communities uc
		ON u.id = uc.user_id
		WHERE uc.community_id IN 
		(
			SELECT community_id
			FROM users_communities
			WHERE users_communities.user_id = id_user_find
		)    
		UNION
		SELECT id
		FROM users 
		WHERE users.id IN (
			(
				SELECT initiator_user_id AS id 
				FROM friend_requests
				WHERE status='approved' 
				AND target_user_id IN (
					SELECT initiator_user_id AS id 
					FROM friend_requests
					WHERE target_user_id = id_user_find AND status='approved'
					UNION ALL
					SELECT target_user_id 
					FROM friend_requests
					WHERE initiator_user_id = id_user_find AND status='approved'
				) 
				UNION
				SELECT target_user_id 
				FROM friend_requests
				WHERE status='approved' 
				AND initiator_user_id IN (
					SELECT initiator_user_id AS id 
					FROM friend_requests
					WHERE target_user_id = id_user_find AND status='approved'
					UNION ALL
					SELECT target_user_id 
					FROM friend_requests
					WHERE initiator_user_id = id_user_find AND status='approved'
				)
			)
		)
	) t
    ORDER BY RAND() 
    LIMIT 5;

END//

CALL 5_users(4);

-- 2. Создать функцию, вычисляющей коэффициент популярности пользователя
DROP FUNCTION IF EXISTS get_popularity_coefficient;
DELIMITER //
CREATE FUNCTION get_popularity_coefficient(
	user_id INT
)
RETURNS INT DETERMINISTIC
BEGIN
	DECLARE result INT DEFAULT 0;
	SELECT 
		(   
			SELECT count(f.id)
			FROM (
				SELECT fr1.initiator_user_id AS id
				FROM friend_requests fr1
				WHERE fr1.target_user_id = u.id AND fr1.status='approved'
				UNION
				SELECT fr2.target_user_id 
				FROM friend_requests fr2
				WHERE fr2.initiator_user_id = u.id AND fr2.status='approved'
			) f
		) AS `count_friends` INTO result
	FROM users u
    WHERE u.id = user_id;
    
    RETURN result;
END//

SELECT get_popularity_coefficient(1); 
