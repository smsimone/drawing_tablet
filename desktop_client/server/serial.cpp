#include "server.hpp"
#include <iostream>
#include <IOKit/IOKitLib.h>
#include <IOKit/usb/IOUSBLib.h>
#include <IOKit/IOCFPlugIn.h>
#include <IOKit/usb/USBSpec.h>
#include <CoreFoundation/CoreFoundation.h>

void SerialServer::list_devices()
{
    CFMutableDictionaryRef classesToMatch = IOServiceMatching(kIOUSBDeviceClassName);
    if (!classesToMatch)
    {
        Logger::error("IOServiceMatching returned NULL");
        return;
    }

    io_iterator_t iter;
    kern_return_t kr = IOServiceGetMatchingServices(kIOMainPortDefault, classesToMatch, &iter);
    if (kr != KERN_SUCCESS)
    {
        Logger::error(std::format("IOServiceGetMatchingServices returned {}", kr));
        return;
    }

    io_object_t device;
    while ((device = IOIteratorNext(iter)) != 0)
    {
        CFTypeRef vendorIDRef = IORegistryEntryCreateCFProperty(device, CFSTR(kUSBVendorID), kCFAllocatorDefault, 0);
        CFTypeRef productIDRef = IORegistryEntryCreateCFProperty(device, CFSTR(kUSBProductID), kCFAllocatorDefault, 0);
        CFTypeRef productNameRef = IORegistryEntryCreateCFProperty(device, CFSTR("USB Product Name"), kCFAllocatorDefault, 0);

        if (vendorIDRef && productIDRef)
        {
            UInt16 vendorId, productId;
            std::string deviceNameValue = "";
            CFNumberGetValue((CFNumberRef)vendorIDRef, kCFNumberSInt16Type, &vendorId);
            CFNumberGetValue((CFNumberRef)productIDRef, kCFNumberSInt16Type, &productId);
            if (productNameRef)
            {
                CFStringRef deviceName = (CFStringRef)productNameRef;
                char deviceNameCString[256];
                CFStringGetCString(deviceName, deviceNameCString, sizeof(deviceNameCString), kCFStringEncodingUTF8);
                CFRelease(productNameRef);
                deviceNameValue = std::string(deviceNameCString);
            }

            Logger::info(std::format("Found device -> {} -- vendorId: {} -- productId: {}", deviceNameValue, vendorId, productId));
        }

        if (vendorIDRef)
            CFRelease(vendorIDRef);
        if (productIDRef)
            CFRelease(productIDRef);
        IOObjectRelease(device);
    }
    IOObjectRelease(iter);
}