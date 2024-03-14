
-- [NCL-8466,NCL-8471] create finallog and family
BEGIN;
CREATE TABLE finallog (
    eventtimestamp timestamp(6) with time zone NOT NULL,
    id bigint NOT NULL,
    logentry_id bigint NOT NULL,
    md5sum character varying(32) NOT NULL,
    loggername character varying(255) NOT NULL,
    logcontent oid
);

ALTER TABLE ONLY finallog ADD CONSTRAINT finallog_pkey PRIMARY KEY (id);

ALTER TABLE ONLY finallog
    ADD CONSTRAINT fkdatr061gftdyjtoegcenya5rg FOREIGN KEY (logentry_id) REFERENCES logentry(id);

CREATE SEQUENCE finallog_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE finallog_tags (
    finallog_id bigint NOT NULL,
    tags character varying(255)
);

ALTER TABLE ONLY finallog_tags
    ADD CONSTRAINT fknt0uv2u9oi71w6ybwbi6h08bf FOREIGN KEY (finallog_id) REFERENCES finallog(id);


CREATE SEQUENCE hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

COMMIT;
