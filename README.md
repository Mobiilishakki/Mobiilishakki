# Mobiilishakki

Tarkoituksena toteuttaaa konenäköön perustuva sovellus, jonka avulla kaksi ihmispelaajaa voivat pelata shakkia fyysisellä laudalla vaikka eivät ole samassa tilassa. Tämä vaatii konenäköratkaisun, joka tunnistaa (kännykkä)kameran kuvavirrasta shakkilaudan, sekä laudalla olevan tilanteen ja siirron. Peli etenee, kun havaittu siirto toimitetaan verkon yli toisella pelaajalle, joka sitten suorittaa fyysisen siirron omalla laudallaan. Yksittäisen pelin kulku tulee tallentaa tietokantaan mahdollistaen pelin myöhemmän esittämisen sekä koneanalyysin. Aihe ei vaadi konenäöstä aiempaa erityistietämystä, sillä toteutuksessa voidaan hyödyntää valmiita vapaan lähdekoodin konenäkökirjastoja (esim. OpenCV - https://opencv.org/). Tunnistuksessa voidaan lähteä oletuksesta, että kamera on mahdollisimman optimaalisesti aseteltu tunnistamista varten.

Lisäominaisuutena voidaan toteuttaa fyysisellä laudalla pelattava peli tietokonetta vastaan käyttäen omaa tai jotakin olemassa olevaa open-source shakkimoottoria (kuten Stockfish - https://stockfishchess.org/). Sekä lisäominaisuutena voidaan tarjota tietokonepohjaista analyysia pelatusta pelistä tai virtuaalista valmentajaa, joka osaisi esimerkiksi vihjata ihmispelaajalle, mitkä siirrot olisivat vaatineet tarkempaa pohdintaa.

Aihe liittyy shakin pelaajakunnan laajentamiseen, siten että digitaalisen shakin edut (riippumattomuus pelaajan fyysisestä sijainnista, pelihistorian automaattinen tallentaminen, tietokoneanalyysi, tietokonevastustaja...) voidaan tuoda pelaajille, jotka enemmän arvostavat fyysistä shakkilauttaa käyttökokemuksena kuin hohtavia ruutuja.



[Backlog](https://docs.google.com/spreadsheets/d/1zG-0s1h2mIXxn2nuR7uvuNg6FT7avz7rtKZj1EuxpaE/edit#gid=1)
