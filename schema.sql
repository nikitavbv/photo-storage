-- we don't know how to generate root <with-no-name> (class Root) :(
create table users
(
	id serial not null
		constraint users_pk
			primary key,
	username varchar(255) not null,
	password_salt varchar(255),
	password_hash varchar(255) not null,
	public_key varchar(512),
	private_key_enc varchar(2000) not null,
	private_key_salt varchar(32) not null
);

create unique index users_id_uindex
	on users (id);

create unique index users_username_uindex
	on users (username);

create unique index users_private_key_salt_uindex
	on users (private_key_salt);

create table sessions
(
	access_token varchar(255) not null
		constraint sessions_pk
			primary key,
	user_id integer not null,
	valid_until bigint not null,
	ip varchar(20) not null,
	user_agent varchar(255) not null
);

create unique index sessions_access_token_uindex
	on sessions (access_token);

create table photos
(
	id varchar(64) not null
		constraint photos_pk
			primary key,
	storage_driver varchar(32) not null,
	storage_key varchar(64) not null,
	upload_timestamp bigint not null,
	description varchar(512),
	location varchar(512),
	tags varchar(512)
);

create unique index photos_id_uindex
	on photos (id);

create table keys
(
	photo_id varchar(64) not null,
	user_id integer not null,
	key_enc varchar(512) not null
);

create table albums
(
	id varchar(256) not null
		constraint albums_pk
			primary key,
	name_enc varchar(512),
	key varchar(512),
	user_id integer
);

create table album_photos
(
	album_id varchar(64),
	photo_id varchar(64)
);
