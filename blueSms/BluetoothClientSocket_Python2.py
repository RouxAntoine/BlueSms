#!/bin/python2
import re

import pyemoji
import json
import socket, sys
import bluetooth
import threading
import signal
import dbus
import dbus.service
from pprint import pprint

contact = None
numCurrentlyKeyIn = False
textCurrentlyKeyIn = False

class th_recv(threading.Thread):
    msg = None
    sock = None
    quit = False

    def __init__(self, s):
        threading.Thread.__init__(self)
        self.sock = s
    
    def parseJson(self):
        o = json.loads(self.msg)
        # print(o["header"]["type"])                                  #   @debug
        # print(o["content"]["num"] + o["content"]["message"])        #   @debug
        if(o['header']['type'] == 'contact'):
            global contact
            contact = o['content']
            contact = remove_duplicates(contact)
            return None, None
        return o["content"]["num"], pyemoji.decode(o["content"]["message"])

    def run(self):
        lenSend = sock.send(bytes("system:!contact!"))
        
        while not self.quit:
            self.receive()
            if self.msg != "":
                if numCurrentlyKeyIn:
                    complement = "\n\nNumero du destinataire : "
                elif textCurrentlyKeyIn:
                    complement = "\n\nTappez votre message : "
                else :
                    complement = "\n"
                num, text = self.parseJson()
                if(num != None and text != None):
                    print("\n   >>>  message recu : \n   De : "+num+"\n   :: "+text+complement)
                else:
                    pprint(contact)
                    print(complement)

    def receive(self):
        self.msg = ""
        try:
            lenToReceive = self.sock.recv(50)
        except ConnectionResetError as e:
            self.quit = True
            return ""

        recu=0
        lenToReceive = lenToReceive.decode("utf-8")

        #case if phone send size and message in same datagram
        regChiffre = re.compile(r"^[0-9]*$")

        if regChiffre.match(lenToReceive) == None:
            lenToReceive,self.msg = lenToReceive.split('{', 1)
            self.msg = '{'+self.msg
            recu = len(self.msg)
        
        while not self.quit and recu < int(lenToReceive):
            try:
                x = self.sock.recv(10)
            except ConnectionResetError as e:
                self.quit = True
                return ""
            recu +=10
            # self.msg += pyemoji.decode(x.decode("utf-8"))
            self.msg += x.decode("latin1")
            # print("msg : "+str(self.msg))
            # print(recu)

    def interupt(self):
        self.quit = True

class MyException(Exception):
    def __init__(self, arg):
        self.value = arg
    
    def __str__(self):
        return repr(self.value)

        

def kill_thread(_sock):
    thReceive.interupt()
    signal.alarm(1)
    thReceive.join()
    sock.close()

def turnOnBluetoothAdapter(state = "on"):
    # dbus-send --system --type=method_call --dest=org.bluez
    #   /org/bluez/hci0
    #   org.freedesktop.DBus.Properties.Set
    #   string:org.bluez.Adapter1
    #   string:Powered
    #   variant:boolean:true
    bus = dbus.SystemBus()
    objBluez = bus.get_object("org.bluez", "/org/bluez/hci0")
    adapter = dbus.Interface(objBluez, "org.freedesktop.DBus.Properties")

    if state == "on" and not adapter.Get('org.bluez.Adapter1','Powered'):
        adapter.Set("org.bluez.Adapter1","Powered", True)
    elif state == "off" and adapter.Get('org.bluez.Adapter1','Powered'):
        adapter.Set("org.bluez.Adapter1","Powered", False)


def nomToNum(nom):
    num = []
    if contact != None:
        for l in contact:
            if nom in l.values():
                num.append(str(l['num']))
        if len(num)==0:
            raise MyException("Contact "+nom+" not found into the phonebook")
    else:
        raise MyException("Contact "+nom+" not found into the phonebook")
    return num

def remove_duplicates(lst, equals=lambda x, y: x == y):
    if not isinstance(lst, list):
        raise TypeError('This function works only with lists.')
    i1 = 0
    l = (len(lst) - 1)
    while i1 < l:
        elem = lst[i1]['nom']
        i2 = i1 + 1
        print("elem : "+elem)
        while i2 <= l:
            if equals(elem, lst[i2]['nom']):
                del lst[i2]
                l -= 1
            else:
                i2 += 1
        i1 += 1
    return lst


def makeAChoice(num):
    while True:
        i = 0
        for n in num:
            print(str(i)+" : "+n)
            i+=1
        c = input("Choisissez le numero que vous souhaitez utiliser : ")
        if int(c) < i:
            break
    
    return num[int(c)]

if __name__ == "__main__":
    # port = 23
    addr = "84:8E:DF:28:89:0C"
    old = [False, False]

    turnOnBluetoothAdapter("on")

    while 1:
        searchBlueSms = bluetooth.find_service(address=addr, name='BlueSms')
        if not searchBlueSms:
            print("Erreur : aucun serveur BlueSms a proximite")
            sys.exit(1)

        for dev in searchBlueSms:
            if dev['name'] == "BlueSms":
                print("Connection to BlueSms server on "+dev['host']+"\nport number "+str(dev['port']))
                port = dev['port']
                addr = dev['host']

        sock=socket.socket( socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM )
        # sock=bluetooth.BluetoothSocket(bluetooth.RFCOMM)
        serv = (addr, port)


        res = sock.connect(serv)
        if res == 0:
            print("connection error")

        thReceive = th_recv(sock)
        thReceive.start()

        while 1:
            try:
                num = ""
                text = ""
                numCurrentlyKeyIn = True
                num = raw_input('Numero ou nom du destinataire : ')
                numCurrentlyKeyIn = False
                textCurrentlyKeyIn = True
                text = raw_input("Tappez votre message : ")
                textCurrentlyKeyIn = False

                old = [num, text]
                if num and text:
                    regNum = re.compile(r"^[0+]([[0-9]-])*")
                    regNom = re.compile(r"^!.*!$")
                    if text == "quit" or text == "!shutdown!":
                        kill_thread(sock)
                        turnOnBluetoothAdapter("off")
                        sys.exit(1)
                    if regNum.match(num) or regNom.match(text):
                        print('num : '+num)
                    else :
                        print('nom : '+num)

                        try:
                            num = nomToNum(num)
                            if len(num) > 1:
                                num = makeAChoice(num)
                            else:
                                num = num[0]
    
                        except MyException as e:
                            print(e)
                            continue
                        
                    lenSend = sock.send(bytes(num+":"+text))
                    print(str(lenSend)+" caractere envoye")
                elif not old[0] and not old[1] and not num and not text:
                    kill_thread(sock)
                    turnOnBluetoothAdapter("off")
                    sys.exit(1)

            except OSError as e:
                print("Fin de la connection le serveur a ete deconnecte !!! ")
                break

        kill_thread(sock)
    
    turnOnBluetoothAdapter("off")

