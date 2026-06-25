SET NAMES utf8mb4;
SET time_zone = '+09:00';
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `emergency_check_evidences`;
DROP TABLE IF EXISTS `user_follows`;
DROP TABLE IF EXISTS `video_bookmarks`;
DROP TABLE IF EXISTS `video_comments`;
DROP TABLE IF EXISTS `community_comments`;
DROP TABLE IF EXISTS `community_posts`;
DROP TABLE IF EXISTS `vet_reports`;
DROP TABLE IF EXISTS `emergency_checks`;
DROP TABLE IF EXISTS `emergency_rules`;
DROP TABLE IF EXISTS `emergency_evidence`;
DROP TABLE IF EXISTS `food_aliases`;
DROP TABLE IF EXISTS `food_safety`;
DROP TABLE IF EXISTS `care_diaries`;
DROP TABLE IF EXISTS `videos`;
DROP TABLE IF EXISTS `pets`;
DROP TABLE IF EXISTS `hospitals`;
DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `login_id` VARCHAR(50) NULL,
  `email` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(50) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `name` VARCHAR(50) NULL,
  `profile_image` LONGTEXT NULL,
  `role` ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_login_id` (`login_id`),
  UNIQUE KEY `uk_users_email` (`email`),
  KEY `idx_users_role` (`role`),
  KEY `idx_users_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `pets` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `breed` VARCHAR(50) NULL,
  `age` INT NULL,
  `weight` DECIMAL(4,1) NULL,
  `gender` ENUM('M','F','UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
  `neutered` BOOLEAN NOT NULL DEFAULT FALSE,
  `allergies` TEXT NULL,
  `diseases` TEXT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pets_user_id` (`user_id`),
  KEY `idx_pets_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_pets_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `food_safety` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `food_name` VARCHAR(100) NOT NULL,
  `risk_level` ENUM('위험','주의','안전','정보부족') NOT NULL,
  `danger_reason` TEXT NULL,
  `observed_symptoms` TEXT NULL,
  `response` TEXT NULL,
  `dose_note` TEXT NULL,
  `risk_condition` TEXT NULL,
  `source_references` JSON NULL,
  `is_immediate_vet` BOOLEAN NOT NULL DEFAULT FALSE,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_food_safety_food_name` (`food_name`),
  KEY `idx_food_safety_risk_level` (`risk_level`),
  KEY `idx_food_safety_immediate_vet` (`is_immediate_vet`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `food_aliases` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `food_id` BIGINT NOT NULL,
  `alias_name` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_food_aliases_alias_name` (`alias_name`),
  KEY `idx_food_aliases_food_id` (`food_id`),
  CONSTRAINT `fk_food_aliases_food` FOREIGN KEY (`food_id`) REFERENCES `food_safety` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `emergency_evidence` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category` VARCHAR(50) NOT NULL,
  `symptom_keyword` VARCHAR(100) NOT NULL,
  `condition_text` TEXT NOT NULL,
  `action_text` TEXT NOT NULL,
  `do_not_action` TEXT NULL,
  `source_title` VARCHAR(300) NOT NULL,
  `source_org` VARCHAR(100) NOT NULL,
  `source_url` VARCHAR(500) NOT NULL,
  `source_type` ENUM('공식기관','전문기관','수의학매뉴얼','병원자료','기타') NOT NULL DEFAULT '전문기관',
  PRIMARY KEY (`id`),
  KEY `idx_evidence_category` (`category`),
  KEY `idx_evidence_symptom_keyword` (`symptom_keyword`),
  KEY `idx_evidence_source_org` (`source_org`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `emergency_rules` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category` VARCHAR(50) NOT NULL,
  `rule_name` VARCHAR(100) NOT NULL,
  `symptom_keywords` JSON NOT NULL,
  `trigger_condition` TEXT NOT NULL,
  `risk_level` ENUM('관찰','주의','위험','정보부족') NOT NULL,
  `is_immediate_vet` BOOLEAN NOT NULL DEFAULT FALSE,
  `recommended_action` TEXT NOT NULL,
  `evidence_id` BIGINT NOT NULL,
  `priority` INT NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rules_category` (`category`),
  KEY `idx_rules_risk_level` (`risk_level`),
  KEY `idx_rules_immediate_vet` (`is_immediate_vet`),
  KEY `idx_rules_priority` (`priority`),
  KEY `idx_rules_evidence_id` (`evidence_id`),
  CONSTRAINT `fk_rules_evidence` FOREIGN KEY (`evidence_id`) REFERENCES `emergency_evidence` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `emergency_checks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `pet_id` BIGINT NOT NULL,
  `occurred_at` DATETIME NULL,
  `repeat_count` TINYINT NOT NULL DEFAULT 0,
  `suspected_food_id` BIGINT NULL,
  `suspected_food_text` VARCHAR(200) NULL,
  `symptom_note` TEXT NOT NULL,
  `symptom_tags` JSON NULL,
  `structured_input` JSON NULL,
  `photo_urls` JSON NULL,
  `risk_level` ENUM('관찰','주의','위험','정보부족') NOT NULL,
  `risk_reason` TEXT NULL,
  `recommended_action` TEXT NULL,
  `analysis_result` TEXT NULL,
  `is_immediate_vet` BOOLEAN NOT NULL DEFAULT FALSE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_checks_user_id` (`user_id`),
  KEY `idx_checks_pet_id` (`pet_id`),
  KEY `idx_checks_food_id` (`suspected_food_id`),
  KEY `idx_checks_risk_level` (`risk_level`),
  KEY `idx_checks_immediate_vet` (`is_immediate_vet`),
  KEY `idx_checks_created_at` (`created_at`),
  CONSTRAINT `fk_checks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_checks_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`),
  CONSTRAINT `fk_checks_food` FOREIGN KEY (`suspected_food_id`) REFERENCES `food_safety` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `vet_reports` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `check_id` BIGINT NOT NULL,
  `risk_summary` TEXT NOT NULL,
  `situation_summary` TEXT NOT NULL,
  `immediate_actions` JSON NULL,
  `avoid_actions` JSON NULL,
  `observation_checklist` JSON NULL,
  `escalation_criteria` JSON NULL,
  `hospital_message` JSON NOT NULL,
  `optional_questions` JSON NULL,
  `evidence_summary` JSON NULL,
  `llm_response_json` JSON NULL,
  `pet_snapshot` JSON NULL,
  `symptom_snapshot` TEXT NULL,
  `pdf_url` VARCHAR(500) NULL,
  `pdf_generated_at` TIMESTAMP NULL DEFAULT NULL,
  `report_status` ENUM('생성완료','PDF생성완료','PDF생성실패') NOT NULL DEFAULT '생성완료',
  `saved_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vet_reports_check_id` (`check_id`),
  KEY `idx_vet_reports_status` (`report_status`),
  KEY `idx_vet_reports_saved_at` (`saved_at`),
  CONSTRAINT `fk_vet_reports_check` FOREIGN KEY (`check_id`) REFERENCES `emergency_checks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `care_diaries` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `pet_id` BIGINT NOT NULL,
  `emergency_check_id` BIGINT NULL,
  `visit_date` DATE NOT NULL,
  `category` ENUM('응급 체크','병원 진료','진료','예방접종','접종','약 복용','검진','케어 루틴') NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `hospital` VARCHAR(200) NULL,
  `memo` TEXT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_care_user_id` (`user_id`),
  KEY `idx_care_pet_id` (`pet_id`),
  KEY `idx_care_visit_date` (`visit_date`),
  KEY `idx_care_deleted_at` (`deleted_at`),
  KEY `idx_care_emergency_check_id` (`emergency_check_id`),
  CONSTRAINT `fk_care_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_care_pet` FOREIGN KEY (`pet_id`) REFERENCES `pets` (`id`),
  CONSTRAINT `fk_care_emergency_check` FOREIGN KEY (`emergency_check_id`) REFERENCES `emergency_checks` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `videos` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_by` BIGINT NULL,
  `updated_by` BIGINT NULL,
  `title` VARCHAR(200) NOT NULL,
  `category` ENUM('CPR/심폐소생술','응급상황 대처','발작/경련','기도폐쇄/하임리히','위험신호/건강체크','구토/설사/소화기 증상','이물섭취/위험물질','호흡기 증상','음식주의/중독','약 복용/투약법') NOT NULL,
  `symptom_tag` VARCHAR(50) NULL,
  `video_description` TEXT NULL,
  `youtube_url` VARCHAR(500) NOT NULL,
  `source` VARCHAR(100) NULL,
  `channel_name` VARCHAR(100) NULL,
  `published_at` DATE NULL,
  `duration_sec` INT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_videos_category` (`category`),
  KEY `idx_videos_symptom_tag` (`symptom_tag`),
  KEY `idx_videos_created_by` (`created_by`),
  KEY `idx_videos_updated_by` (`updated_by`),
  CONSTRAINT `fk_videos_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_videos_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `hospitals` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `management_number` VARCHAR(30) NOT NULL,
  `hospital_name` VARCHAR(200) NOT NULL,
  `address` VARCHAR(500) NOT NULL,
  `phone` VARCHAR(20) NULL,
  `lat` DECIMAL(10,7) NULL,
  `lng` DECIMAL(10,7) NULL,
  `is_24h` BOOLEAN NOT NULL DEFAULT FALSE,
  `opening_hours` JSON NULL,
  `emergency_available` BOOLEAN NULL,
  `night_available` BOOLEAN NULL,
  `holiday_available` BOOLEAN NULL,
  `operating_status` VARCHAR(50) NULL,
  `kakao_place_id` VARCHAR(100) NULL,
  `google_place_id` VARCHAR(255) NULL,
  `last_verified_at` TIMESTAMP NULL DEFAULT NULL,
  `source_modified_at` TIMESTAMP NULL DEFAULT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hospitals_management_number` (`management_number`),
  UNIQUE KEY `uk_hospitals_kakao_place_id` (`kakao_place_id`),
  UNIQUE KEY `uk_hospitals_google_place_id` (`google_place_id`),
  KEY `idx_hospitals_coordinates` (`lat`,`lng`),
  KEY `idx_hospitals_operating_status` (`operating_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `community_posts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `category` VARCHAR(50) NOT NULL,
  `title` VARCHAR(300) NOT NULL,
  `content` TEXT NOT NULL,
  `image_urls` JSON NULL,
  `view_count` INT NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_posts_user_id` (`user_id`),
  KEY `idx_posts_category` (`category`),
  KEY `idx_posts_created_at` (`created_at`),
  KEY `idx_posts_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_posts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `community_comments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL,
  `parent_id` BIGINT NULL,
  `user_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_community_comments_post_id` (`post_id`),
  KEY `idx_community_comments_parent_id` (`parent_id`),
  KEY `idx_community_comments_user_id` (`user_id`),
  KEY `idx_community_comments_created_at` (`created_at`),
  KEY `idx_community_comments_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_community_comments_post` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_community_comments_parent` FOREIGN KEY (`parent_id`) REFERENCES `community_comments` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_community_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `video_comments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `video_id` BIGINT NOT NULL,
  `parent_id` BIGINT NULL,
  `user_id` BIGINT NOT NULL,
  `rating` TINYINT NOT NULL DEFAULT 5,
  `content` TEXT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_video_comments_video_id` (`video_id`),
  KEY `idx_video_comments_parent_id` (`parent_id`),
  KEY `idx_video_comments_user_id` (`user_id`),
  KEY `idx_video_comments_created_at` (`created_at`),
  KEY `idx_video_comments_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_video_comments_video` FOREIGN KEY (`video_id`) REFERENCES `videos` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_video_comments_parent` FOREIGN KEY (`parent_id`) REFERENCES `video_comments` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_video_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_video_comments_rating` CHECK (`rating` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `video_bookmarks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `video_id` BIGINT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_bookmarks_user_video` (`user_id`,`video_id`),
  KEY `idx_video_bookmarks_video_id` (`video_id`),
  CONSTRAINT `fk_video_bookmarks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_video_bookmarks_video` FOREIGN KEY (`video_id`) REFERENCES `videos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `emergency_check_evidences` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `check_id` BIGINT NOT NULL,
  `evidence_id` BIGINT NOT NULL,
  `rule_id` BIGINT NOT NULL,
  `match_score` DECIMAL(5,2) NULL,
  `matched_keywords` JSON NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_check_evidence_rule` (`check_id`,`evidence_id`,`rule_id`),
  KEY `idx_check_evidences_evidence_id` (`evidence_id`),
  KEY `idx_check_evidences_rule_id` (`rule_id`),
  CONSTRAINT `fk_check_evidences_check` FOREIGN KEY (`check_id`) REFERENCES `emergency_checks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_check_evidences_evidence` FOREIGN KEY (`evidence_id`) REFERENCES `emergency_evidence` (`id`),
  CONSTRAINT `fk_check_evidences_rule` FOREIGN KEY (`rule_id`) REFERENCES `emergency_rules` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_follows` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `follower_id` BIGINT NOT NULL,
  `following_id` BIGINT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follows_pair` (`follower_id`,`following_id`),
  KEY `idx_user_follows_following_id` (`following_id`),
  CONSTRAINT `chk_user_follows_not_self` CHECK (`follower_id` <> `following_id`),
  CONSTRAINT `fk_user_follows_follower` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_follows_following` FOREIGN KEY (`following_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
