-- Manual cleanup for APIs removed from the app:
-- measurements, size recommendations, and user preferences.
--
-- Hibernate ddl-auto=update does not drop old tables or columns, so run this
-- once against existing development databases after deploying this change.

ALTER TABLE fitting_results
    DROP COLUMN measurement_id,
    DROP COLUMN recommendation_id,
    DROP COLUMN recommended_size;

ALTER TABLE try_on_results
    DROP COLUMN measurement_id;

DROP TABLE IF EXISTS size_recommendations;
DROP TABLE IF EXISTS user_preferences;
DROP TABLE IF EXISTS measurements;
