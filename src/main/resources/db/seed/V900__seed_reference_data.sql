INSERT INTO speciality (name, description) VALUES
    ('Medicina Geral e Familiar', 'Primeiro contacto com o utente e acompanhamento continuado ao longo da vida.'),
    ('Cardiologia',               'Diagnóstico e tratamento das doenças do coração e dos vasos.'),
    ('Pediatria',                 'Acompanhamento médico de crianças e adolescentes.'),
    ('Ginecologia-Obstetrícia',   'Saúde do aparelho reprodutor feminino, gravidez e parto.'),
    ('Ortopedia',                 'Lesões e doenças dos ossos, articulações e músculos.'),
    ('Urologia',                  'Doenças do aparelho urinário e do aparelho reprodutor masculino.'),
    ('Oftalmologia',              'Diagnóstico e tratamento das doenças dos olhos e da visão.'),
    ('Otorrinolaringologia',      'Doenças do ouvido, nariz, garganta e estruturas associadas.'),
    ('Psiquiatria',               'Diagnóstico e tratamento das perturbações mentais e do comportamento.'),
    ('Medicina Interna',          'Abordagem global das doenças do adulto, sobretudo as que envolvem vários órgãos.');

INSERT INTO room (room_number) VALUES
    ('Sala 1'), ('Sala 2'), ('Sala 3'), ('Sala 4'),
    ('Sala 5'), ('Sala 6'), ('Sala 7'), ('Sala 8');

INSERT INTO doctor (name, email, working_hours_start, working_hours_end) VALUES
    ('Ana Ribeiro',     'ana.ribeiro@example.com',     '08:00', '14:00'),
    ('Bruno Carvalho',  'bruno.carvalho@example.com',  '09:00', '18:00'),
    ('Catarina Lopes',  'catarina.lopes@example.com',  '09:00', '18:00'),
    ('Diogo Fonseca',   'diogo.fonseca@example.com',   '09:00', '18:00'),
    ('Eduarda Matos',   'eduarda.matos@example.com',   '09:00', '18:00'),
    ('Filipe Antunes',  'filipe.antunes@example.com',  '09:00', '18:00'),
    ('Helena Cruz',     'helena.cruz@example.com',     '09:00', '18:00'),
    ('Ivo Marques',     'ivo.marques@example.com',     '09:00', '18:00'),
    ('Joana Pinto',     'joana.pinto@example.com',     '09:00', '18:00'),
    ('Luis Teixeira',   'luis.teixeira@example.com',   '09:00', '18:00');

INSERT INTO doctor_working_day (doctor_id, day_of_week)
SELECT d.id, v.day_of_week
FROM (VALUES
    ('ana.ribeiro@example.com',    'MONDAY'),
    ('ana.ribeiro@example.com',    'TUESDAY'),
    ('ana.ribeiro@example.com',    'WEDNESDAY'),
    ('ana.ribeiro@example.com',    'THURSDAY'),
    ('ana.ribeiro@example.com',    'FRIDAY'),
    ('bruno.carvalho@example.com', 'MONDAY'),
    ('bruno.carvalho@example.com', 'TUESDAY'),
    ('bruno.carvalho@example.com', 'WEDNESDAY'),
    ('bruno.carvalho@example.com', 'THURSDAY'),
    ('bruno.carvalho@example.com', 'FRIDAY'),
    ('catarina.lopes@example.com', 'MONDAY'),
    ('catarina.lopes@example.com', 'TUESDAY'),
    ('catarina.lopes@example.com', 'WEDNESDAY'),
    ('catarina.lopes@example.com', 'THURSDAY'),
    ('catarina.lopes@example.com', 'FRIDAY'),
    ('diogo.fonseca@example.com',  'MONDAY'),
    ('diogo.fonseca@example.com',  'TUESDAY'),
    ('diogo.fonseca@example.com',  'WEDNESDAY'),
    ('diogo.fonseca@example.com',  'THURSDAY'),
    ('diogo.fonseca@example.com',  'FRIDAY'),
    ('eduarda.matos@example.com',  'MONDAY'),
    ('eduarda.matos@example.com',  'TUESDAY'),
    ('eduarda.matos@example.com',  'WEDNESDAY'),
    ('eduarda.matos@example.com',  'THURSDAY'),
    ('eduarda.matos@example.com',  'FRIDAY'),
    ('filipe.antunes@example.com', 'MONDAY'),
    ('filipe.antunes@example.com', 'TUESDAY'),
    ('filipe.antunes@example.com', 'WEDNESDAY'),
    ('filipe.antunes@example.com', 'THURSDAY'),
    ('filipe.antunes@example.com', 'FRIDAY'),
    ('helena.cruz@example.com',    'MONDAY'),
    ('helena.cruz@example.com',    'TUESDAY'),
    ('helena.cruz@example.com',    'WEDNESDAY'),
    ('helena.cruz@example.com',    'THURSDAY'),
    ('helena.cruz@example.com',    'FRIDAY'),
    ('ivo.marques@example.com',    'TUESDAY'),
    ('ivo.marques@example.com',    'WEDNESDAY'),
    ('ivo.marques@example.com',    'THURSDAY'),
    ('ivo.marques@example.com',    'FRIDAY'),
    ('ivo.marques@example.com',    'SATURDAY'),
    ('joana.pinto@example.com',    'MONDAY'),
    ('joana.pinto@example.com',    'TUESDAY'),
    ('joana.pinto@example.com',    'WEDNESDAY'),
    ('joana.pinto@example.com',    'THURSDAY'),
    ('joana.pinto@example.com',    'FRIDAY'),
    ('luis.teixeira@example.com',  'MONDAY'),
    ('luis.teixeira@example.com',  'TUESDAY'),
    ('luis.teixeira@example.com',  'WEDNESDAY'),
    ('luis.teixeira@example.com',  'THURSDAY'),
    ('luis.teixeira@example.com',  'FRIDAY')
) AS v(email, day_of_week)
JOIN doctor d ON d.email = v.email;

INSERT INTO doctor_speciality (doctor_id, speciality_id)
SELECT d.id, s.id
FROM (VALUES
    ('ana.ribeiro@example.com',    'Medicina Geral e Familiar'),
    ('bruno.carvalho@example.com', 'Cardiologia'),
    ('bruno.carvalho@example.com', 'Medicina Interna'),
    ('bruno.carvalho@example.com', 'Medicina Geral e Familiar'),
    ('catarina.lopes@example.com', 'Pediatria'),
    ('catarina.lopes@example.com', 'Medicina Geral e Familiar'),
    ('diogo.fonseca@example.com',  'Ginecologia-Obstetrícia'),
    ('eduarda.matos@example.com',  'Ortopedia'),
    ('eduarda.matos@example.com',  'Medicina Geral e Familiar'),
    ('filipe.antunes@example.com', 'Urologia'),
    ('helena.cruz@example.com',    'Psiquiatria'),
    ('ivo.marques@example.com',    'Otorrinolaringologia'),
    ('joana.pinto@example.com',    'Psiquiatria'),
    ('joana.pinto@example.com',    'Medicina Geral e Familiar'),
    ('luis.teixeira@example.com',  'Medicina Interna'),
    ('luis.teixeira@example.com',  'Cardiologia'),
    ('luis.teixeira@example.com',  'Medicina Geral e Familiar')
) AS v(email, speciality_name)
JOIN doctor d     ON d.email = v.email
JOIN speciality s ON s.name = v.speciality_name;

INSERT INTO patient (sns_number, name, date_of_birth, email, phone) VALUES
    ('0001', 'Miguel Sousa',        '1984-03-12', 'miguel.sousa@example.com',        '+351 912 345 601'),
    ('0002', 'Sofia Almeida',       '1991-11-27', 'sofia.almeida@example.com',       '+351 912 345 602'),
    ('0003', 'Rui Barbosa',         '1976-07-04', 'rui.barbosa@example.com',         '+351 912 345 603'),
    ('0004', 'Ines Cardoso',        '1998-01-19', 'ines.cardoso@example.com',        '+351 912 345 604'),
    ('0005', 'Tiago Moreira',       '1969-09-30', 'tiago.moreira@example.com',       '+351 912 345 605'),
    ('0006', 'Beatriz Nogueira',    '2002-05-08', 'beatriz.nogueira@example.com',    '+351 912 345 606'),
    ('0007', 'Andre Coelho',        '1988-12-15', 'andre.coelho@example.com',        '+351 912 345 607'),
    ('0008', 'Mariana Freitas',     '1995-02-23', 'mariana.freitas@example.com',     '+351 912 345 608'),
    ('0009', 'Pedro Vasconcelos',   '1972-06-11', 'pedro.vasconcelos@example.com',   '+351 912 345 609'),
    ('0010', 'Carolina Rocha',      '2016-04-02', 'carolina.rocha@example.com',      '+351 912 345 610'),
    ('0011', 'Nuno Figueiredo',     '1980-10-21', 'nuno.figueiredo@example.com',     '+351 912 345 611'),
    ('0012', 'Rita Amaral',         '1993-08-17', 'rita.amaral@example.com',         '+351 912 345 612'),
    ('0013', 'Goncalo Pereira',     '1965-01-05', 'goncalo.pereira@example.com',     '+351 912 345 613'),
    ('0014', 'Leonor Baptista',     '2018-09-14', 'leonor.baptista@example.com',     '+351 912 345 614'),
    ('0015', 'Vasco Henriques',     '1987-03-29', 'vasco.henriques@example.com',     '+351 912 345 615'),
    ('0016', 'Margarida Costa',     '1974-12-06', 'margarida.costa@example.com',     '+351 912 345 616'),
    ('0017', 'Hugo Neves',          '1999-07-23', 'hugo.neves@example.com',          '+351 912 345 617'),
    ('0018', 'Daniela Correia',     '1990-05-31', 'daniela.correia@example.com',     '+351 912 345 618'),
    ('0019', 'Ricardo Tavares',     '1961-02-09', 'ricardo.tavares@example.com',     '+351 912 345 619'),
    ('0020', 'Claudia Monteiro',    '2011-10-18', 'claudia.monteiro@example.com',    '+351 912 345 620');

INSERT INTO appointment (patient_id, doctor_id, room_id, speciality_id, start_time)
SELECT p.id, d.id, r.id, s.id, v.start_time
FROM (VALUES
    ('miguel.sousa@example.com',    'bruno.carvalho@example.com', 'Sala 1', 'Cardiologia',          TIMESTAMPTZ '2026-09-01 08:30:00+00'),
    ('sofia.almeida@example.com',   'ana.ribeiro@example.com',    'Sala 2', 'Medicina Geral e Familiar', TIMESTAMPTZ '2026-09-02 09:00:00+00'),
    ('leonor.baptista@example.com', 'catarina.lopes@example.com', 'Sala 3', 'Pediatria',            TIMESTAMPTZ '2026-09-03 10:30:00+00'),
    ('ines.cardoso@example.com',    'helena.cruz@example.com',    'Sala 4', 'Psiquiatria',          TIMESTAMPTZ '2026-09-04 14:00:00+00'),
    ('rui.barbosa@example.com',     'ivo.marques@example.com',    'Sala 5', 'Otorrinolaringologia', TIMESTAMPTZ '2026-09-05 08:30:00+00')
) AS v(patient_email, doctor_email, room_number, speciality_name, start_time)
JOIN patient p    ON p.email = v.patient_email
JOIN doctor d     ON d.email = v.doctor_email
JOIN room r       ON r.room_number = v.room_number
JOIN speciality s ON s.name = v.speciality_name;
