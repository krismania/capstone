insert ignore into `vehicles`
	(`registration`, `make`, `model`, `year`, `colour`, `location`)
values
    ("ABC123","Toyota", "Corolla", 2014, "Blue", POINT(-37.808401, 144.956159)),
    ("QRB990","BMW", "325i", 2003, "Black", POINT(-37.809741, 144.970895)),
    ("TAA325","Peugeot", "307 SW", 2008, "Grey", POINT(-37.805819, 144.960025)),
    ("UBR666","Ford", "Falcon", 2013, "Orange", POINT(-37.815603, 144.969967)),
    ("FOK356","Holden", "Barina", 2017, "White", POINT(-37.814022, 144.961954)),
    ("JTD955","Holden", "Commadore", 2005, "Grey", POINT(-37.816170, 144.956179)),
    ("BLA555","Mazda", "3", 2010, "White", POINT(-37.818681, 144.958982)),
    ("QOP299","Kia", "Rio", 2013, "Pink", POINT(-37.811510, 144.965667)),
    ("YODUDE","Nissan", "Skyline", 2010, "Black", POINT(-37.810422, 144.968597)),
    ("MAGPIES", "Mercedes-Benz", "CLC200 Kompressor", 2009, "Black", POINT(-37.807232, 144.963620));

insert ignore into `bookings`
	(`timestamp`, `registration`, `customer_id`, `duration`, `start_location`, `end_location`)
values
	("2018-04-02 13:11:00", "YODUDE", "s3489609@student.rmit.edu.au", 120, POINT(-37.815603, 144.969967), POINT(-37.807232, 144.963620)),
    ("2018-04-02 13:11:00", "ABC123", "s3489609@student.rmit.edu.au", 720, POINT(-37.814022, 144.961954), POINT(-37.808401, 144.956159)),
    ("2018-04-03 20:04:00", "UBR666", "s3543819@student.rmit.edu.au", 60, POINT(-37.810422, 144.968597), POINT(-37.815603, 144.969967)),
    ("2018-04-05 15:45:00", "YODUDE", "s3543819@student.rmit.edu.au", 180, POINT(-37.807232, 144.963620), POINT(-37.810422, 144.968597)),
    ("2018-04-05 15:45:00", "MAGPIES", "s3540620@student.rmit.edu.au", 360, POINT(-37.815603, 144.969967), POINT(-37.807232, 144.963620)),
    ("2018-04-02 13:11:00", "QOP299", "s3540620@student.rmit.edu.au", 60, POINT(-37.814022, 144.961954), POINT(-37.808401, 144.956159)),
    ("2018-04-03 20:04:00", "JTD955", "s3607187@student.rmit.edu.au", 120, POINT(-37.810422, 144.968597), POINT(-37.815603, 144.969967)),
    ("2018-04-05 15:45:00", "FOK356", "s3607187@student.rmit.edu.au", 180, POINT(-37.807232, 144.963620), POINT(-37.810422, 144.968597));

insert ignore into `admins`
	(`admin_id`)
values   
    ("102908696637872505288"),
    ("MARTIN"),
    ("KYR"),
    ("112606983151403770748");
