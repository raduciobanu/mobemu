Description of the public data traces of iMotes experiment deployment in
Cambridge.

========================
I Terms and conditions: 
========================

- You are welcome to use these traces for your research. We ask you in
return to accept the following terms:
 
 1- To acknowledge the use of the data in all resulting publications. 

Please reference the following:
 
 @InProceedings{cam-leguay06,
 author = {Jérémie Leguay and Anders Lindgren and James Scott and Timur Friedman and Jon Crowcroft},
 title = {Opportunistic Content Distribution in an Urban Setting},
 booktitle = {Proc. {ACM SIGCOMM} 2006 - Workshop on Challenged Networks ({CHANTS})},
 month = {September}
 year = {2006},
 address = {Pisa, Italy}
 }
 
 the paper is accessible online from here:
 http://www-rp.lip6.fr/site_npa/site_rp/_publications/697-chants06.pdf

 2- Not to redistribute the data set to anyone without our permission.
 
 3- Not to use the data for any other purpose than education and research.

 ====
 NB: In order to gain access to this file and the data we provide, you
     should have already confirmed these terms to us by e-mail. If this is
     not already the case, please send us just an e-mail to confirm.

========================
II Data collection and pre-processing: 
========================

We tried to keep the processing of data before public release to a
minimum, to allow any flexibility for possible research use. Some
choices had to be made to reduce power consumption, memory use, and
because of specific capabilities of the iMote prototype. 
Before using these data for your research, it may be important to
check that it does not impact any of your findings.

1- periodic desynchronized scanning.

In our experiment, iMotes were distributed to a group of people to collect
any opportunistic sighting of other Bluetooth devices (including the other
iMotes distributed). Each iMote scans on a periodic basis for devices,
asking them to respond with their MAC address, via the paging function.

It takes approximately 5 to 10s to perform the complete scanning. After
initial tests, we observe that most of the contacts were recorded with a
5s scanning time, and this value was used in the experiment.

The time granularity between two scanning is Ns. Later in this document,
the exact values we chose are given. It is important to avoid
synchronization of two iMotes around the same cycle clock, as each of them
cannot respond to any request when it is actively scanning. Therefore, we
implemented a random dephasing on [-12s;+12s] to handle this case.

2- skip-length sequence.

A contact "A sees B" is defined as a period of time where all
successive scanning by A receive a positive answer by B. Ideally an
information should be kept at the end of each contact period. 

After preliminary test it became quite clear that a very large number of
contact periods were only separated by one interval. We decided, to avoid
memory overflow, to implement a skip sequence of "one", meaning that a
contact period will only be stopped after two successive failure of a
scanning response. As a consequence, no inter-contact time of less than
two intervals could have been observed.

3- Manual Time synchronization.

Time between iMotes is not synchronized by a central entity, and traces
belonging to different devices bear times which are relative to the
starting time of each device. We recorded the time at which each iMote was
first powered up, which corresponds to time 0 at that iMote. After
collecting the data, we then converted all times into Unix timestamps
(seconds elapsed since 00:00:00 UTC, Jan 1, 1970).

4- Corrupted MAC address, and discarded mote.

As in the Haggle experiments, we observed that a number of MAC addresses
recorded were different from a known one only by one or two digit. They
were most of the time recorded once for a single time slot. It is clear
that at least a part of them comes for a corrupted signal received on the
link level by our devices. to ignore this artificial data, we implement
the following rule:

"Any MAC address that were recorded only once, for a single scanning (that
is, related with a unique contact, with length 1s), should be supposed
defective and ignored." We did not discard any iMotes in these data set.  
We recommend to remove iMotes that were seen only one time for a contact
of length 1s.

5- Anonymization and Address Identifier.

To protect participants privacy, we choose not to release the MAC address,
neither from the iMotes nor from other external devices recorded. Every
device is given a unique identifier, usually called ID number in this
document. Depending on which number, it might be an iMote or another MAC
address that were recorded from other active Bluetooth devices around.

========================
III iMotes deployment
========================

In the experiment we performed, we were interested in tracking contacts
between different mobile users, and also contacts between mobile users and
various fixed locations.

Mobile users in our experiment mainly consisted of students from Cambridge
University who were asked to carry these iMotes with them at all times for
the duration of the experiment. In addition to this, we deployed a number
of stationary nodes in various locations that we expected many people to
visit such as grocery stores, pubs, market places, and shopping centers in
and around the city of Cambridge, UK. A stationary iMote was also placed
at the reception of the Computer Lab, in which most of the experiment
participants are students.

Here are the different types of iMotes that we deployed:

MSR-10 : Mobile Short Range iMotes with an interval of 10 minutes between
inquiries. These iMotes were given to a group of 40 students, mostly in
the 3rd year at the Cambridge University Computer Lab. The devices were
packaged in small boxes (dental floss boxes) to be easy to carry around in
a pocket, and used a CR-2 battery (950 mAh) for power.

FSR-10 : Fixed Short Range iMotes with an interval of 10 minutes between
inquiries. We deployed 15 of these iMotes in fixed locations such as pubs,
shops or colleges' porter lodges. We used exactly the same packaging and
batteries as the MSR-10.

FSR-6 : Fixed Short Range iMotes with an inquiry interval of 6 minutes.
These iMotes were equipped with a more powerful rechargeable battery
providing 2200 mAh so that we were able to reduce the inquiry interval to
6 minutes. We deployed 2 of these.

FLR-2 : Fixed Long Range iMotes with an interval of 2 minutes between
inquiries. To increase the area in which these iMotes can discover other
devices, four devices were equipped with an external antenna,
which provided a communication range that was approximately twice that of
the short range iMotes. Furthermore, these iMotes were also equipped with
3 more powerful rechargeable batteries providing 2200 mAh so that we could
reduced the inquiry interval to 2 minutes.

The experiment started on Friday, October 28th 2005, 9:55:32 (GMT)
and stopped on Wednesday, December 21th 2005, 13:00 (GMT).

Due to various hardware problems and the loss of some of the deployed
iMotes, we were able to gather measurement data from 36 mobile
participants and 18 fixed locations.

========================
IV Description of the files in each experiment
========================

=====
"MAC3Btable"
is a file that contains the three first bytes of the MAC address,
associated with each ID. It could be useful to identify the manufacturer
of each external device.

Note that MAC devices from ID=11168 to ID=11421 should be removed because
they may correspond to fake devices. This is the results from MAC
corruption. According to the OUI (Organizationally Unique Identifier)
database we could not have MAC addresses that begin with the first bytes
higher than 0x08.

=====
"*.dat"
are files describing the contact recorded by all devices we distributed
during this experiment.

The dat file N.dat represents the data for the iMote with identifier (ID)
N.  These data files for the 3 different categories of iMotes are in the
following directories:
- SR-10mins-FixLocation
- SR-10mins-Students
- SR-6mins-FixLocation
- LR-2mins 

========================
Examples taken from LR-2mins/37.dat 
========================
9546 1130504701 1130504701
10536 1130505044 1130505044
4649 1130506372 1130506372
7490 1130506608 1130506615
5905 1130506851 1130506851
8996 1130506851 1130506858
1431 1130506970 1130506970
5639 1130507327 1130507327
6883 1130508255 1130508255
6540 1130508606 1130508613
========================
========================

- The first column gives the ID of the device who was seen by the iMote 37. 
- The second and third columns describe, respectively, the first and
last time when the address were recorded for the contact. 

- Note, again, that these contacts may not be mutual between a pair of
iMotes, because scanning period of different iMotes are not
synchronized, and because the sightings might not be symmetric.
- Also, times are unix timestamps which correspond to the number of seconds
since midnight January 1, 1970 UTC (referred to as the Epoch). 

Globally, the ID have been attributed in the following fashion:
- SR-10mins-Students ( ID in [1:36] )
- LR-2mins ( ID in [37:40] )
- SR-10mins-FixLocation ( ID in [41:52] )
- SR-6mins-FixLocation ( ID in [53:54] )
- External contacts ( ID in [55:inf] )

To ease the understanding of data while keeping a sufficent privacy level, 
we provide here an idea of the kind of locations where fixed iMotes were deployed:

Pubs: 41, 45, 46, 47, 50 Shop windows: 37, 39, 42, 43, 44, 48, 49, 53Popular supermarket: 38Central point in the commercial center n°1: 52Central point in the commercial center n°2: 40
College porter's lodge: 51Computer lab reception:	54


========================
IV Answer to some additional questions
========================

- Do you have any location or speed data ?

The answer is no. We did only gather data on the contacts between
mobile devices, that is an indication of their proximity.
In that sense, they may not be considered as classical mobility
traces, focusing on geographical position or speed. 

========================
V Feedback
========================

Feedback on this data is extremely welcomed. Please do not hesitate
to contact one of us if you have any question, or need any extra
information about the experimental settings.

========================
VI Acknowledgements
========================

We would like to thank all the participants of the experiments and we
especially acknowledge Pan Hui for his useful support. He helped us to
program and package the iMotes.
Furthermore, we would like to Pan Hui, Augustin Chaintreau and 
James Scott for letting us the possibility to adapt their
original README.txt for this iMote experiment in Cambridge.

|===============================================|
|	Jeremie Leguay <jeremie.leguay@lip6.fr>,	|
|	Anders Lindgren <dugdale@sm.luth.se>		|
|===============================================|
