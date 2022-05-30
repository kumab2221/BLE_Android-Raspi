from pybleno import *


APPROACH_SERVICE_UUID = '13A28130-8883-49A8-8BDB-42BC1A7107F4'
APPROACH_CHARACTERISTIC_UUID1 = 'A2935077-201F-44EB-82E8-10CC02AD8CE1'
APPROACH_CHARACTERISTIC_UUID2 = 'A2935077-201F-44EB-82E8-10CC02AD8CE2'


class ApproachCharacteristic(Characteristic):

    def __init__(self, uuid):
        Characteristic.__init__(self, {
            'uuid': uuid,
            'properties': ['read', 'notify'],
            'value': None
        })

        self._value = str(0).encode()
        self._updateValueCallback = None

    def onReadRequest(self, offset, callback):
        print('ApproachCharacteristic - onReadRequest')
        callback(result=Characteristic.RESULT_SUCCESS, data=self._value)

    def onSubscribe(self, maxValueSize, updateValueCallback):
        print('ApproachCharacteristic - onSubscribe')

        self._updateValueCallback = updateValueCallback

    def onUnsubscribe(self):
        print('ApproachCharacteristic - onUnsubscribe')

        self._updateValueCallback = None


def onStateChange(state):
    print('on -> stateChange: ' + state)

    if (state == 'poweredOn'):
        bleno.startAdvertising(name='Approach', service_uuids=[APPROACH_SERVICE_UUID])
    else:
        bleno.stopAdvertising()


def onAdvertisingStart(error):
    print('on -> advertisingStart: ' + ('error ' + error if error else 'success'))

    if not error:
        bleno.setServices([
            BlenoPrimaryService({
                'uuid': APPROACH_SERVICE_UUID,
                'characteristics': [
                    approachCharacteristic1,
                    approachCharacteristic2
                ]
            })
        ])


import time

bleno = Bleno()
bleno.on('stateChange', onStateChange)

approachCharacteristic1 = ApproachCharacteristic(APPROACH_CHARACTERISTIC_UUID1)
approachCharacteristic2 = ApproachCharacteristic(APPROACH_CHARACTERISTIC_UUID2)
bleno.on('advertisingStart', onAdvertisingStart)

bleno.start()

counter = 0

use_input = 1

while True:
    counter += 1
    approachCharacteristic1._value = str(counter).encode()

    if use_input == 1:
        #approachCharacteristic2._value = str(counter).encode()
        approachCharacteristic2._value = input().encode()

    if approachCharacteristic1._updateValueCallback:

        print('Sending notification with value : ' + str(approachCharacteristic1._value))

        notificationBytes = str(approachCharacteristic1._value).encode()
        approachCharacteristic1._updateValueCallback(data=notificationBytes)

    if approachCharacteristic2._updateValueCallback:

        print('Sending notification with value : ' + str(approachCharacteristic2._value))

        notificationBytes = str(approachCharacteristic2._value).encode()
        approachCharacteristic2._updateValueCallback(data=notificationBytes)
    time.sleep(1)
    #input()


