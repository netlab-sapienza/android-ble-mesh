
BE Mesh: A Bluetooth Low Energy Mesh Network
[![Twitter URL](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/MeshBle)
===================================

<img align="left" src="https://www.uniroma1.it/sites/default/files/images/logo/sapienza-big.png"/>
<img align="right" src="https://infocom2019.ieee-infocom.org/sites/infocom2019.ieee-infocom.org/files/ieee-infocom2.png"/>

<br><br><br><br><br><br><br><br>


***Andrea Lacava∗, Gianluigi Nero∗, Pierluigi Locatelli∗, Francesca Cuomo∗, Tommaso Melodia†***

***∗University of Rome “La Sapienza”, 00184, ITALY; †Northeastern University, MA 02115, USA***

*Abstract* - We   propose   and   discuss BE-Mesh-Bluetooth low Energy-Meshed network,  a  new  paradigm  for  BLE  (BluetoothLow  Energy)  that  enables  mesh  networking  among  wirelessly interconnected   devices,   both   in   a   single   hop   and   multi-hop fashion.  Starting  from  the  classical  Master/Slave  paradigm  of Bluetooth,  we  build  two  new  layers  based  on  BLE  stack  that allow the final user to set-up, in a fast way, the desired network topology while hiding the complexity and low-level details of the BLE  stack.  We  also  prototype,  as  a  proof  of  concept,  an  open source  Android  library that  implements  our  communication paradigm  and  an  Android  application  that  allows  the  exchange of text messages across the mesh network. Last, we demonstrate how  BE-Mesh  enables  Internet  access  sharing  with  the  whole mesh  from  a  single  Internet-connected  device.

- Follow our Official BE Mesh Twitter account: [@MeshBle](https://twitter.com/MeshBle).
- This work was presented at [Infocom 2019](https://infocom2019.ieee-infocom.org/postersdemos).
- Lower tier of [HIRO-NET](https://github.com/HIRO-NET-Emergency-Network/HIRO-NET).
- Read the [demo paper](https://ieeexplore.ieee.org/document/8845084).
- If you intend to use this work, please cite the related paper (BibTex):

```
@INPROCEEDINGS{Laca1904:Demo,
AUTHOR="Andrea Lacava and Gianluigi Nero and Pierluigi Locatelli and Francesca
Cuomo and Tommaso Melodia",
TITLE="Demo Abstract: {BE-Mesh:} Bluetooth Low Energy Mesh Networking",
BOOKTITLE="2019 IEEE INFOCOM Demo (INFOCOM 2019 Demo)",
ADDRESS="Paris, France",
DAYS=28,
MONTH=apr,
YEAR=2019,
KEYWORDS="Bluetooth Low Energy; Android library; Mesh network; Internet access
sharing",
ABSTRACT="We propose and discuss BE-Mesh-Bluetooth low Energy-Meshed network, a new
paradigm for BLE (Bluetooth Low Energy) that enables mesh networking among
wirelessly interconnected devices, both in a single hop and multi-hop
fashion. Starting from the classical Master/Slave paradigm of Bluetooth, we
build two new layers based on BLE stack that allow the final user to
set-up, in a fast way, the desired network topology while hiding the
complexity and low-level details of the BLE stack. We also prototype, as a
proof of concept, an open source Android library [1] that implements our
communication paradigm and an Android application that allows the exchange
of text messages across the mesh network. Last, we demonstrate how BE-Mesh
enables Internet access sharing with the whole mesh from a single
Internet-connected device."
}
```

# Create a Be Mesh!

1. [Download](https://play.google.com/store/apps/details?id=it.drone.mesh) and open the app
2. Press the start button
3. Chat with the others
4. Try to send an email or a tweet


# Build the app from source

Please note that the current version of the repository does not reflect the version onthe google play store and compatibility between versions is not guaranteed.


## Pre-requisites

- The phone candidate to be server must support multipleAdvertisement
- Android SDK >= 23
- Compatible with Android Things!


- Clone this repository
- You must create two files in *app/src/main/resources* with the following attributes:

   **email.properties:**


```
email.username=
email.password=

```    
    
   **twitter4j.properties:**

```
debug=true
oauth.consumerKey=
oauth.consumerSecret=
oauth.accessToken=
oauth.accessTokenSecret=
```

- Build the project and launch the app on your smartphone 
