/*
 * NOTIFYICONDATA.java
 *
 * Created on 2. Mrz 2007, 11:49
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xvolks.jnative.misc.basicStructures;

import java.io.UnsupportedEncodingException;
import org.xvolks.jnative.pointers.*;
import org.xvolks.jnative.exceptions.*;
import org.xvolks.jnative.misc.WindowsVersion;
import org.xvolks.jnative.pointers.memory.MemoryBlockFactory;

/**
 *
 * @author osthop
 */
public class NOTIFYICONDATA extends AbstractBasicData<NOTIFYICONDATA> {
  
    public static final int NIF_MESSAGE = 0x01;
    public static final int NIF_ICON = 0x02;
    public static final int NIF_TIP = 0x04;
    public static final int NIF_STATE = 0x08;
    public static final int NIF_INFO = 0x10;

    public static final int NIM_ADD = 0x00;
    public static final int NIM_MODIFY = 0x01;
    public static final int NIM_DELETE = 0x02;
    public static final int NIM_SETFOCUS = 0x03;
    public static final int NIM_SETVERSION = 0x04;

    public static final int NOTIFYICON_VERSION = 0x03;
    
    public static final int NOTIFYICONDATAA_V1_SIZE_A = 92;
    public static final int NOTIFYICONDATAA_V1_SIZE_U = 156;
    public static final int NOTIFYICONDATAA_V2_SIZE_A = 488;
    public static final int NOTIFYICONDATAA_V2_SIZE_U = 936;
            
    public static final int WM_MOUSEMOVE = 0x200;
    public static final int WM_LBUTTONDBLCLK = 0x203;
    public static final int WM_LBUTTONDOWN = 0x201;
    public static final int WM_LBUTTONUP = 0x202;
    public static final int WM_RBUTTONDBLCLK = 0x206;
    public static final int WM_RBUTTONDOWN = 0x204;
    public static final int WM_RBUTTONUP = 0x205;
    public static final int WM_NCHITTEST = 0x84;

    public static final int WM_USER = 0x400;

    public static final int NIN_SELECT = WM_USER;
    public static final int NINF_KEY =  0x01;
    public static final int NIN_KEYSELECT =  (NIN_SELECT | NINF_KEY);
    public static final int NIN_BALLOONSHOW =  (WM_USER + 2);
    public static final int NIN_BALLOONHIDE =  (WM_USER + 3);
    public static final int NIN_BALLOONTIMEOUT = (WM_USER + 4);
    public static final int NIN_BALLOONUSERCLICK =  (WM_USER + 5);
    
        /*
    UNICODE:
    
    cbSize As Long             ' 4
    hWnd As Long               ' 8
    uID As Long                ' 12
    uFlags As Long             ' 16
    uCallbackMessage As Long   ' 20
    hIcon As Long              ' 24
    szTip(0 To 255) As Byte    ' 280
    dwState As Long            ' 284
    dwStateMask As Long        ' 288
    szInfo(0 To 511) As Byte   ' 800
    uTimeOutOrVersion As Long  ' 804
    szInfoTitle(0 To 127) As Byte ' 932
    dwInfoFlags As Long        ' 936
    guidItem As Long           ' 940

    ANSI:
    
    cbSize As Long             ' 4
    hWnd As Long               ' 8
    uID As Long                ' 12
    uFlags As Long             ' 16
    uCallbackMessage As Long   ' 20
    hIcon As Long              ' 24
    szTip As String * 128      ' 152
    dwState As Long            ' 156
    dwStateMask As Long        ' 160
    szInfo As String * 256     ' 416
    uTimeOutOrVersion As Long  ' 420
    szInfoTitle As String * 64 ' 484
    dwInfoFlags As Long        ' 488
    guidItem As Long           ' 492
    */
   
    private int cbSize;
    public int hWnd;
    public int uID;
    public int uFlags;
    public int uCallbackMessage;
    public int hIcon;
    public String szTip = "";
    public int dwState;
    public int dwStateMask;
    public String szInfo = "";
    public int uTimeOutOrVersion;
    public String szInfoTitle = "";
    public int dwInfoFlags;
    public int guidItem;
    
    public enum ICON_TYPE {
        NIIF_NONE(0x00),
        NIIF_INFO(0x01),
        NIIF_WARNING(0x02),
        NIIF_ERROR(0x03),
        NIIF_NOSOUND(0x10);

        private int value;

        private ICON_TYPE(int value) {
            this.value = value;
        }
        
        public int getValue() {
        	return value;
        }
    }

    public NOTIFYICONDATA() {
        super(null);
        try {
            cbSize = getSizeOf();
            createPointer();
        } catch (NativeException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}		
    }
    
    public Pointer createPointer() throws NativeException {
        pointer = new Pointer(MemoryBlockFactory.createMemoryBlock(getSizeOf()));
		return pointer;
    } 
    public NOTIFYICONDATA getValueFromPointer() throws NativeException {
        return this;
    }
    
    // TODO: add switch for !WindowsVersion.supportsNewVersion()
    public Pointer getPointer() {
        if(WindowsVersion.supportsUnicode()) {
            try {
                byte[] _szTip = szTip.substring(0, (szTip.length() <= 255) ? szTip.length() : 255 ).getBytes("UTF-16LE");
                byte[] _szInfo = szInfo.substring(0, (szInfo.length() <= 255) ? szInfo.length() : 255 ).getBytes("UTF-16LE");
                byte[] _szInfoTitle = szInfoTitle.substring(0, (szInfoTitle.length() <= 255) ? szInfoTitle.length() : 255 ).getBytes("UTF-16LE");
                
                pointer.setIntAt(0,cbSize);
                pointer.setIntAt(4,hWnd);
                pointer.setIntAt(8,uID);
                pointer.setIntAt(12,uFlags);
                pointer.setIntAt(16,uCallbackMessage);
                pointer.setIntAt(20,hIcon);
                for(int i = 0; i<_szTip.length; i++)
                    pointer.setByteAt(24+i, _szTip[i]);

                pointer.setIntAt(280,dwState);
                pointer.setIntAt(284,dwStateMask);
                
                for(int i = 0; i<_szInfo.length; i++)
                    pointer.setByteAt(288+i, _szInfo[i]);

                pointer.setIntAt(800,uTimeOutOrVersion);
                
                for(int i = 0; i<_szInfoTitle.length; i++)
                    pointer.setByteAt(804+i, _szInfoTitle[i]);

                pointer.setIntAt(932,dwInfoFlags);
                //pointer.setIntAt(936,guidItem);
            } catch(UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch(NativeException e) {
                e.printStackTrace();
            }
        } else {
            try {
                pointer.setIntAt(0,cbSize);
                pointer.setIntAt(4,hWnd);
                pointer.setIntAt(8,uID);
                pointer.setIntAt(12,uFlags);
                pointer.setIntAt(16,uCallbackMessage);
                pointer.setIntAt(20,hIcon);
                pointer.setStringAt(24,szTip.substring(0, (szTip.length() < 128) ? szTip.length() : 127 ));
                pointer.setIntAt(152,dwState);
                pointer.setIntAt(156,dwStateMask);
                pointer.setStringAt(160,szInfo.substring(0, (szInfo.length() < 256) ? szInfo.length() : 255 ));
                pointer.setIntAt(416,uTimeOutOrVersion);
                pointer.setStringAt(420,szInfoTitle.substring(0, (szInfoTitle.length() < 64) ? szInfoTitle.length() : 63 ));
                pointer.setIntAt(484,dwInfoFlags);
                //pointer.setIntAt(488,guidItem);
            } catch(NativeException e) {
                e.printStackTrace();
            }
        }
        return pointer;
    }
    
    public int getSizeOf() {
        if(WindowsVersion.supportsUnicode()) {
            if(WindowsVersion.supportsNewVersion())
                return NOTIFYICONDATAA_V2_SIZE_U;
            else
                return NOTIFYICONDATAA_V1_SIZE_U;
        } else {
            if(WindowsVersion.supportsNewVersion())
                return NOTIFYICONDATAA_V2_SIZE_A;
            else
                return NOTIFYICONDATAA_V1_SIZE_A;
        }
    }   
    
    public String toString() {
        String ret = "Icondata:\n";
        ret += "cbSize: "+cbSize+"\n";
        ret += "hWnd: "+hWnd+"\n";
        ret += "uID: "+uID+"\n";
        ret += "uFlags: "+uFlags+"\n";
        ret += "uCallbackMessage: "+uCallbackMessage+"\n";
        ret += "hIcon: "+hIcon+"\n";
        ret += "szTip: "+ szTip+"\n";
        ret += "dwState: "+ dwState+"\n";
        ret += "dwStateMask: "+ dwStateMask+"\n";
        ret += "uTimeOutOrVersion: "+ uTimeOutOrVersion+"\n";
        ret += "szInfoTitle: "+ szInfoTitle+"\n";
        ret += "dwInfoFlags: "+ dwInfoFlags+"\n";
        ret += "guidItem: "+ guidItem+"\n";
        return ret;
    }
}
